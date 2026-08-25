package com.chc.dpgb.library.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    private final LibraryBookRepository libraryBookRepository;
    private final ShelfRepository shelfRepository;
    private final ShelfService shelfService;
    private final ScrapService scrapService;

    LibraryBookService(
            LibraryBookRepository libraryBookRepository,
            ShelfRepository shelfRepository,
            ShelfService shelfService,
            ScrapService scrapService
    ) {
        this.libraryBookRepository = libraryBookRepository;
        this.shelfRepository = shelfRepository;
        this.shelfService = shelfService;
        this.scrapService = scrapService;
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
        if (isbn != null && libraryBookRepository.existsByIsbn(memberId, isbn)) {
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

        try {
            return libraryBookRepository.save(book);
        } catch (DataIntegrityViolationException e) {
            throw new BookAlreadyRegisteredException();
        }
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
        scrapService.softDeleteAllByBookId(bookId, now);
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
        List<LibraryBook> books = libraryBookRepository.findShelfOrderedByRank(shelfId);
        String[] newRanks = ShelfRank.rebalancedSequence(books.size());
        for (int i = 0; i < books.size(); i++) {
            books.get(i).changeShelfRank(newRanks[i]);
            libraryBookRepository.save(books.get(i));
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
