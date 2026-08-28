package com.chc.dpgb.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.chc.dpgb.library.domain.LibraryBook;

interface LibraryBookJpaRepository extends JpaRepository<LibraryBook, Long> {

    List<LibraryBook> findByShelfIdOrderByShelfRankAsc(Long shelfId);

    Optional<LibraryBook> findTopByShelfIdOrderByShelfRankDesc(Long shelfId);

    Optional<LibraryBook> findByMemberIdAndIsbn(UUID memberId, String isbn);

    long countByShelfId(Long shelfId);

    @Query("""
            SELECT lb FROM LibraryBook lb
            WHERE lb.memberId = :memberId
              AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
              AND (:author IS NULL OR lb.author = :author)
            """)
    Page<LibraryBook> findPage(UUID memberId, Long shelfId, String author, Pageable pageable);

    @Query("""
            SELECT lb FROM LibraryBook lb
            WHERE lb.memberId = :memberId
              AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
              AND (:author IS NULL OR lb.author = :author)
            ORDER BY (lb.currentPage * 1.0 / lb.totalPages) ASC
            """)
    Page<LibraryBook> findPageOrderByProgressAsc(
            UUID memberId, Long shelfId, String author, Pageable pageable
    );

    @Query("""
            SELECT lb FROM LibraryBook lb
            WHERE lb.memberId = :memberId
              AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
              AND (:author IS NULL OR lb.author = :author)
            ORDER BY (lb.currentPage * 1.0 / lb.totalPages) DESC
            """)
    Page<LibraryBook> findPageOrderByProgressDesc(
            UUID memberId, Long shelfId, String author, Pageable pageable
    );
}
