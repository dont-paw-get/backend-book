package com.chc.dpgb.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.chc.dpgb.library.application.LibraryBookRepository;
import com.chc.dpgb.library.domain.LibraryBook;

@Repository
class LibraryBookRepositoryJpaAdapter implements LibraryBookRepository {

    private final LibraryBookJpaRepository jpaRepository;

    LibraryBookRepositoryJpaAdapter(LibraryBookJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LibraryBook save(LibraryBook libraryBook) {
        return jpaRepository.saveAndFlush(libraryBook);
    }

    @Override
    public Optional<LibraryBook> findById(Long bookId) {
        return jpaRepository.findById(bookId);
    }

    @Override
    public List<LibraryBook> findShelfOrderedByRank(Long shelfId) {
        return jpaRepository.findByShelfIdOrderByShelfRankAsc(shelfId);
    }

    @Override
    public Optional<LibraryBook> findLastRanked(Long shelfId) {
        return jpaRepository.findTopByShelfIdOrderByShelfRankDesc(shelfId);
    }

    @Override
    public boolean existsByIsbn(UUID memberId, String isbn) {
        return jpaRepository.existsByMemberIdAndIsbn(memberId, isbn);
    }

    @Override
    public long countByShelfId(Long shelfId) {
        return jpaRepository.countByShelfId(shelfId);
    }

    @Override
    public Page<LibraryBook> findPage(
            UUID memberId, Long shelfId, String author, Pageable pageable
    ) {
        return jpaRepository.findPage(memberId, shelfId, author, pageable);
    }

    @Override
    public Page<LibraryBook> findPageOrderByProgress(
            UUID memberId, Long shelfId, String author, boolean ascending, Pageable pageable
    ) {
        return ascending
                ? jpaRepository.findPageOrderByProgressAsc(memberId, shelfId, author, pageable)
                : jpaRepository.findPageOrderByProgressDesc(memberId, shelfId, author, pageable);
    }
}
