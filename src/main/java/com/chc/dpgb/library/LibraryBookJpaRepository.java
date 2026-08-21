package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface LibraryBookJpaRepository extends JpaRepository<LibraryBook, Long> {

	List<LibraryBook> findByShelfIdOrderByShelfRankAsc(Long shelfId);

	Optional<LibraryBook> findTopByShelfIdOrderByShelfRankDesc(Long shelfId);

	boolean existsByMemberIdAndIsbn(String memberId, String isbn);

	long countByShelfId(Long shelfId);

	@Query("""
			SELECT lb FROM LibraryBook lb
			WHERE lb.memberId = :memberId
			  AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
			  AND (:author IS NULL OR lb.author = :author)
			""")
	Page<LibraryBook> findPage(String memberId, Long shelfId, String author, Pageable pageable);

	@Query("""
			SELECT lb FROM LibraryBook lb
			WHERE lb.memberId = :memberId
			  AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
			  AND (:author IS NULL OR lb.author = :author)
			ORDER BY (lb.currentPage * 1.0 / lb.totalPages) ASC
			""")
	Page<LibraryBook> findPageOrderByProgressAsc(String memberId, Long shelfId, String author, Pageable pageable);

	@Query("""
			SELECT lb FROM LibraryBook lb
			WHERE lb.memberId = :memberId
			  AND (:shelfId IS NULL OR lb.shelfId = :shelfId)
			  AND (:author IS NULL OR lb.author = :author)
			ORDER BY (lb.currentPage * 1.0 / lb.totalPages) DESC
			""")
	Page<LibraryBook> findPageOrderByProgressDesc(String memberId, Long shelfId, String author, Pageable pageable);
}
