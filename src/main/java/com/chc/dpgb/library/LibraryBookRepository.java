package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LibraryBookRepository {

    LibraryBook save(LibraryBook libraryBook);

    void delete(LibraryBook libraryBook);

    Optional<LibraryBook> findById(Long bookId);

    List<LibraryBook> findShelfOrderedByRank(Long shelfId);

    Optional<LibraryBook> findLastRanked(Long shelfId);

    boolean existsByIsbn(String memberId, String isbn);

    long countByShelfId(Long shelfId);

    Page<LibraryBook> findPage(String memberId, Long shelfId, String author, Pageable pageable);

    Page<LibraryBook> findPageOrderByProgress(
            String memberId, Long shelfId, String author, boolean ascending, Pageable pageable
    );
}
