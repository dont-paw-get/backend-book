package com.chc.dpgb.library.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.common.exception.BookAlreadyRegisteredException;
import com.chc.dpgb.common.exception.InvalidBookDataException;
import com.chc.dpgb.common.exception.InvalidPageValueException;
import com.chc.dpgb.common.exception.InvalidReorderTargetException;
import com.chc.dpgb.common.exception.InvalidShelfTargetException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;
import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;
import com.chc.dpgb.library.domain.Shelf;
import com.chc.dpgb.library.domain.ShelfRank;
import com.chc.dpgb.library.domain.ShelfRankExhaustedException;

@Service
public class LibraryBookService {

    private static final Logger log = LoggerFactory.getLogger(LibraryBookService.class);

    /**
     * rebalance는 드물게 일어나지만 한 요청 안에서 책장 전체를 다시 저장한다. 자동 계측만 보면 평소와 똑같은 요청이 갑자기
     * 수십 건의 UPDATE span을 쏟아낸 것처럼 보이므로, 그 이유를 span 이름으로 드러낸다.
     */
    private static final String REBALANCE_OBSERVATION_NAME = "library.shelf.rebalance";
    private static final String REBALANCE_BOOK_COUNT_KEY = "library.shelf.book_count";

    private final LibraryBookRepository libraryBookRepository;
    private final ShelfRepository shelfRepository;
    private final ShelfService shelfService;
    private final ScrapService scrapService;
    private final ObservationRegistry observationRegistry;

    LibraryBookService(
            LibraryBookRepository libraryBookRepository,
            ShelfRepository shelfRepository,
            ShelfService shelfService,
            ScrapService scrapService,
            ObservationRegistry observationRegistry
    ) {
        this.libraryBookRepository = libraryBookRepository;
        this.shelfRepository = shelfRepository;
        this.shelfService = shelfService;
        this.scrapService = scrapService;
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    public LibraryBook createLibraryBook(
            UUID memberId,
            Long requestedShelfId,
            String title,
            String author,
            String isbn,
            Genre genre,
            String publisher,
            LocalDate publishedDate,
            String coverUrl,
            ReadingStatus readingStatus,
            Integer totalPages
    ) {
        Shelf shelf = resolveShelf(memberId, requestedShelfId);
        if (isbn != null && libraryBookRepository.findByMemberIdAndIsbn(memberId, isbn).isPresent()) {
            throw new BookAlreadyRegisteredException();
        }
        String shelfRank = rankAfterWithRebalance(shelf.getShelfId());

        LibraryBook book;
        try {
            book = LibraryBook.register(
                    memberId,
                    shelf.getShelfId(),
                    shelfRank,
                    title,
                    author,
                    isbn,
                    genre,
                    publisher,
                    publishedDate,
                    coverUrl,
                    readingStatus,
                    totalPages
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidBookDataException(e.getMessage());
        }

        LibraryBook saved;
        try {
            saved = libraryBookRepository.save(book);
        } catch (DataIntegrityViolationException e) {
            // 위 findByMemberIdAndIsbn 검사를 통과했는데도 unique 제약에 걸렸다는 뜻 — 동시 등록 경합이다.
            log.warn(
                    "서재 책 등록 경합으로 중복 판정 shelfId={}", shelf.getShelfId()
            );
            throw new BookAlreadyRegisteredException();
        }
        log.info(
                "서재 책 등록 bookId={} shelfId={} hasIsbn={}",
                saved.getBookId(), saved.getShelfId(), isbn != null
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<LibraryBook> getLibraryBooks(
            UUID memberId,
            Long shelfId,
            String author,
            LibrarySortBy sortBy,
            Sort.Direction direction,
            int page,
            int size
    ) {
        if (sortBy == LibrarySortBy.PROGRESS) {
            Pageable pageable = PageRequest.of(page, size);
            return libraryBookRepository.findPageOrderByProgress(
                    memberId,
                    shelfId,
                    author,
                    direction == Sort.Direction.ASC,
                    pageable
            );
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, attributeFor(sortBy)));
        return libraryBookRepository.findPage(memberId, shelfId, author, pageable);
    }

    @Transactional(readOnly = true)
    public LibraryBook getLibraryBook(UUID memberId, Long bookId) {
        return getOwnedBook(memberId, bookId);
    }

    @Transactional
    public LibraryBook updateLibraryBook(
            UUID memberId,
            Long bookId,
            String title,
            String author,
            String isbn,
            Genre genre,
            String publisher,
            LocalDate publishedDate,
            String coverUrl,
            ReadingStatus readingStatus,
            Integer totalPages
    ) {
        LibraryBook book = getOwnedBook(memberId, bookId);
        try {
            book.updateMetadata(title, author, isbn, genre, publisher, publishedDate, coverUrl, readingStatus, totalPages);
        } catch (IllegalArgumentException e) {
            throw new InvalidBookDataException(e.getMessage());
        }
        return libraryBookRepository.save(book);
    }

    @Transactional
    public void deleteLibraryBook(UUID memberId, Long bookId) {
        LibraryBook book = getOwnedBook(memberId, bookId);
        Instant now = Instant.now();
        book.softDelete(now);
        libraryBookRepository.save(book);
        int deletedScraps = scrapService.softDeleteAllByBookId(bookId, now);
        log.info("서재 책 삭제 bookId={} deletedScraps={}", bookId, deletedScraps);
    }

    @Transactional
    public LibraryBook reorderLibraryBook(
            UUID memberId,
            Long bookId,
            Long beforeBookId,
            Long afterBookId
    ) {
        if ((beforeBookId == null) == (afterBookId == null)) {
            throw new InvalidReorderTargetException();
        }
        LibraryBook book = getOwnedBook(memberId, bookId);
        Long neighborId = beforeBookId != null ? beforeBookId : afterBookId;
        boolean placeBefore = beforeBookId != null;

        if (neighborId.equals(bookId)) {
            throw new InvalidReorderTargetException();
        }
        LibraryBook neighbor = libraryBookRepository.findById(neighborId)
                .orElseThrow(InvalidReorderTargetException::new);
        if (!neighbor.getShelfId().equals(book.getShelfId())) {
            throw new InvalidReorderTargetException();
        }

        String newRank = rankNextToWithRebalance(book.getShelfId(), bookId, neighborId, placeBefore);
        book.changeShelfRank(newRank);
        return libraryBookRepository.save(book);
    }

    @Transactional
    public LibraryBook moveLibraryBookToShelf(UUID memberId, Long bookId, Long targetShelfId) {
        LibraryBook book = getOwnedBook(memberId, bookId);
        Shelf targetShelf = shelfRepository.findById(targetShelfId)
                .filter(shelf -> shelf.getMemberId().equals(memberId))
                .orElseThrow(InvalidShelfTargetException::new);
        String newRank = rankAfterWithRebalance(targetShelf.getShelfId());
        book.changeShelfId(targetShelf.getShelfId(), newRank);
        return libraryBookRepository.save(book);
    }

    @Transactional
    public LibraryBook updateReadingProgress(
            UUID memberId,
            Long bookId,
            int currentPage,
            Integer totalPages
    ) {
        LibraryBook book = getOwnedBook(memberId, bookId);
        try {
            book.updateProgress(currentPage, totalPages);
        } catch (IllegalArgumentException e) {
            throw new InvalidPageValueException(e.getMessage());
        }
        return libraryBookRepository.save(book);
    }

    private Shelf resolveShelf(UUID memberId, Long requestedShelfId) {
        if (requestedShelfId == null) {
            return shelfService.getOrCreateDefaultShelf(memberId);
        }
        return shelfRepository.findById(requestedShelfId)
                .filter(shelf -> shelf.getMemberId().equals(memberId))
                .orElseThrow(InvalidBookDataException::new);
    }

    private LibraryBook getOwnedBook(UUID memberId, Long bookId) {
        LibraryBook book = libraryBookRepository.findById(bookId)
                .orElseThrow(LibraryBookNotFoundException::new);
        if (!book.getMemberId().equals(memberId)) {
            throw new LibraryBookAccessDeniedException();
        }
        return book;
    }

    private String rankAfterWithRebalance(Long shelfId) {
        return libraryBookRepository.findLastRanked(shelfId)
                .map(last -> {
                    try {
                        return ShelfRank.after(last.getShelfRank());
                    } catch (ShelfRankExhaustedException e) {
                        rebalanceShelf(shelfId);
                        LibraryBook newLast = libraryBookRepository.findLastRanked(shelfId).orElseThrow();
                        return ShelfRank.after(newLast.getShelfRank());
                    }
                })
                .orElseGet(ShelfRank::initial);
    }

    private String rankNextToWithRebalance(
            Long shelfId,
            Long movingBookId,
            Long neighborId,
            boolean placeBefore
    ) {
        List<LibraryBook> ordered = libraryBookRepository.findShelfOrderedByRank(shelfId);
        ordered.removeIf(b -> b.getBookId().equals(movingBookId));
        int index = indexOf(ordered, neighborId);
        String prev = placeBefore
                ? (index > 0 ? ordered.get(index - 1).getShelfRank() : null)
                : ordered.get(index).getShelfRank();
        String next = placeBefore
                ? ordered.get(index).getShelfRank()
                : (index < ordered.size() - 1 ? ordered.get(index + 1).getShelfRank() : null);
        try {
            return ShelfRank.between(prev, next);
        } catch (ShelfRankExhaustedException e) {
            rebalanceShelf(shelfId);
            return rankNextToWithRebalance(shelfId, movingBookId, neighborId, placeBefore);
        }
    }

    private void rebalanceShelf(Long shelfId) {
        Observation observation = Observation.start(REBALANCE_OBSERVATION_NAME, observationRegistry);
        try (Observation.Scope ignored = observation.openScope()) {
            List<LibraryBook> books = libraryBookRepository.findShelfOrderedByRank(shelfId);
            // 책 수는 값의 종류가 많아 메트릭 태그로는 부적절하다 — span 속성으로만 남긴다.
            observation.highCardinalityKeyValue(REBALANCE_BOOK_COUNT_KEY, String.valueOf(books.size()));
            log.warn(
                    "shelfRank 키 공간 소진으로 책장 재정렬 shelfId={} bookCount={}", shelfId, books.size()
            );
            String[] newRanks = ShelfRank.rebalancedSequence(books.size());
            for (int i = 0; i < books.size(); i++) {
                books.get(i).changeShelfRank(newRanks[i]);
                libraryBookRepository.save(books.get(i));
            }
        } catch (RuntimeException e) {
            observation.error(e);
            throw e;
        } finally {
            observation.stop();
        }
    }

    private static int indexOf(List<LibraryBook> books, Long bookId) {
        for (int i = 0; i < books.size(); i++) {
            if (books.get(i).getBookId().equals(bookId)) {
                return i;
            }
        }
        throw new InvalidReorderTargetException();
    }

    private static String attributeFor(LibrarySortBy sortBy) {
        return switch (sortBy) {
            case SHELF_ORDER -> "shelfRank";
            case TITLE -> "title";
            case AUTHOR -> "author";
            case CREATED_AT -> "createdAt";
            case PROGRESS -> throw new IllegalStateException("PROGRESS는 별도 쿼리로 처리한다");
        };
    }
}
