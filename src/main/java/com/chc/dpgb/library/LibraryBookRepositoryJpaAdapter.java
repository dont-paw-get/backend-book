package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
class LibraryBookRepositoryJpaAdapter implements LibraryBookRepository {

	private final LibraryBookJpaRepository jpaRepository;

	LibraryBookRepositoryJpaAdapter(LibraryBookJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public LibraryBook save(LibraryBook libraryBook) {
		return jpaRepository.save(libraryBook);
	}

	@Override
	public Optional<LibraryBook> findOwnedBook(Long bookId, String memberId) {
		return jpaRepository.findByBookIdAndMemberId(bookId, memberId);
	}

	@Override
	public List<LibraryBook> findShelfOrderedByRank(String memberId) {
		return jpaRepository.findByMemberIdOrderByShelfRankAsc(memberId);
	}

	@Override
	public Optional<LibraryBook> findLastRanked(String memberId) {
		return jpaRepository.findTopByMemberIdOrderByShelfRankDesc(memberId);
	}

	@Override
	public boolean existsByIsbn(String memberId, String isbn) {
		return jpaRepository.existsByMemberIdAndIsbn(memberId, isbn);
	}
}
