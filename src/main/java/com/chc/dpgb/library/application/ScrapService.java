package com.chc.dpgb.library.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.common.exception.InvalidScrapDataException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;
import com.chc.dpgb.common.exception.ScrapAccessDeniedException;
import com.chc.dpgb.common.exception.ScrapNotFoundException;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Scrap;

@Service
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final LibraryBookRepository libraryBookRepository;

    ScrapService(ScrapRepository scrapRepository, LibraryBookRepository libraryBookRepository) {
        this.scrapRepository = scrapRepository;
        this.libraryBookRepository = libraryBookRepository;
    }

    @Transactional
    public Scrap createScrap(
            UUID memberId, Long bookId, String sentence, Integer pageNumber, String scrapImageUrl, String memo
    ) {
        getOwnedBook(memberId, bookId);
        try {
            return scrapRepository.save(Scrap.create(bookId, sentence, pageNumber, scrapImageUrl, memo));
        } catch (IllegalArgumentException e) {
            throw new InvalidScrapDataException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Scrap> getScraps(UUID memberId, Long bookId, int page, int size) {
        getOwnedBook(memberId, bookId);
        Pageable pageable = PageRequest.of(page, size);
        return scrapRepository.findPageByBookId(bookId, pageable);
    }

    @Transactional(readOnly = true)
    public Scrap getScrap(UUID memberId, Long scrapId) {
        return getOwnedScrap(memberId, scrapId);
    }

    @Transactional
    public Scrap updateScrap(
            UUID memberId, Long scrapId, String sentence, Integer pageNumber, String scrapImageUrl, String memo
    ) {
        Scrap scrap = getOwnedScrap(memberId, scrapId);
        try {
            scrap.update(sentence, pageNumber, scrapImageUrl, memo);
        } catch (IllegalArgumentException e) {
            throw new InvalidScrapDataException(e.getMessage());
        }
        return scrapRepository.save(scrap);
    }

    @Transactional
    public void deleteScrap(UUID memberId, Long scrapId) {
        Scrap scrap = getOwnedScrap(memberId, scrapId);
        scrap.softDelete(Instant.now());
        scrapRepository.save(scrap);
    }

    /**
     * @return 함께 논리 삭제된 스크랩 수 — 호출자({@code LibraryBookService.deleteLibraryBook})가 캐스케이드 범위를
     *         로그로 남기는 데 쓴다.
     */
    @Transactional
    int softDeleteAllByBookId(Long bookId, Instant deletedAt) {
        List<Scrap> scraps = scrapRepository.findAllByBookId(bookId);
        for (Scrap scrap : scraps) {
            scrap.softDelete(deletedAt);
            scrapRepository.save(scrap);
        }
        return scraps.size();
    }

    private LibraryBook getOwnedBook(UUID memberId, Long bookId) {
        LibraryBook book = libraryBookRepository.findById(bookId)
                .orElseThrow(LibraryBookNotFoundException::new);
        if (!book.getMemberId().equals(memberId)) {
            throw new LibraryBookAccessDeniedException();
        }
        return book;
    }

    private Scrap getOwnedScrap(UUID memberId, Long scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId).orElseThrow(ScrapNotFoundException::new);
        LibraryBook book = libraryBookRepository.findById(scrap.getBookId())
                .orElseThrow(ScrapNotFoundException::new);
        if (!book.getMemberId().equals(memberId)) {
            throw new ScrapAccessDeniedException();
        }
        return scrap;
    }
}
