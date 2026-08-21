package com.chc.dpgb.library;

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

@Service
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final LibraryBookRepository libraryBookRepository;

    ScrapService(ScrapRepository scrapRepository, LibraryBookRepository libraryBookRepository) {
        this.scrapRepository = scrapRepository;
        this.libraryBookRepository = libraryBookRepository;
    }

    @Transactional
    public Scrap createScrap(String memberId, Long bookId, String sentence, Integer pageNumber, String memo) {
        getOwnedBook(memberId, bookId);
        try {
            return scrapRepository.save(Scrap.create(bookId, sentence, pageNumber, memo));
        } catch (IllegalArgumentException e) {
            throw new InvalidScrapDataException(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Scrap> getScraps(String memberId, Long bookId, int page, int size) {
        getOwnedBook(memberId, bookId);
        Pageable pageable = PageRequest.of(page, size);
        return scrapRepository.findPageByBookId(bookId, pageable);
    }

    @Transactional(readOnly = true)
    public Scrap getScrap(String memberId, Long scrapId) {
        return getOwnedScrap(memberId, scrapId);
    }

    @Transactional
    public Scrap updateScrap(String memberId, Long scrapId, String sentence, Integer pageNumber, String memo) {
        Scrap scrap = getOwnedScrap(memberId, scrapId);
        try {
            scrap.update(sentence, pageNumber, memo);
        } catch (IllegalArgumentException e) {
            throw new InvalidScrapDataException(e.getMessage());
        }
        return scrapRepository.save(scrap);
    }

    @Transactional
    public void deleteScrap(String memberId, Long scrapId) {
        Scrap scrap = getOwnedScrap(memberId, scrapId);
        scrapRepository.delete(scrap);
    }

    private LibraryBook getOwnedBook(String memberId, Long bookId) {
        LibraryBook book = libraryBookRepository.findById(bookId)
                .orElseThrow(LibraryBookNotFoundException::new);
        if (!book.getMemberId().equals(memberId)) {
            throw new LibraryBookAccessDeniedException();
        }
        return book;
    }

    private Scrap getOwnedScrap(String memberId, Long scrapId) {
        Scrap scrap = scrapRepository.findById(scrapId).orElseThrow(ScrapNotFoundException::new);
        LibraryBook book = libraryBookRepository.findById(scrap.getBookId())
                .orElseThrow(ScrapNotFoundException::new);
        if (!book.getMemberId().equals(memberId)) {
            throw new ScrapAccessDeniedException();
        }
        return scrap;
    }
}
