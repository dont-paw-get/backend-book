package com.chc.dpgb.library.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.chc.dpgb.library.domain.LibraryBook;

public interface LibraryBookRepository {

    LibraryBook save(LibraryBook libraryBook);

    Optional<LibraryBook> findById(Long bookId);

    List<LibraryBook> findShelfOrderedByRank(Long shelfId);

    Optional<LibraryBook> findLastRanked(Long shelfId);

    boolean existsByIsbn(UUID memberId, String isbn);

    long countByShelfId(Long shelfId);

    Page<LibraryBook> findPage(UUID memberId, Long shelfId, String author, Pageable pageable);

    Page<LibraryBook> findPageOrderByProgress(
            UUID memberId, Long shelfId, String author, boolean ascending, Pageable pageable
    );
}
