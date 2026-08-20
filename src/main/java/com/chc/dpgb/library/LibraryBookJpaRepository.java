package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface LibraryBookJpaRepository extends JpaRepository<LibraryBook, Long> {

	Optional<LibraryBook> findByBookIdAndMemberId(Long bookId, String memberId);

	List<LibraryBook> findByMemberIdOrderByShelfRankAsc(String memberId);

	Optional<LibraryBook> findTopByMemberIdOrderByShelfRankDesc(String memberId);

	boolean existsByMemberIdAndIsbn(String memberId, String isbn);
}
