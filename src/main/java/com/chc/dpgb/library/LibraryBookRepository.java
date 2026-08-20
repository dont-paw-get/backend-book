package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

public interface LibraryBookRepository {

	LibraryBook save(LibraryBook libraryBook);

	Optional<LibraryBook> findOwnedBook(Long bookId, String memberId);

	List<LibraryBook> findShelfOrderedByRank(String memberId);

	Optional<LibraryBook> findLastRanked(String memberId);

	boolean existsByIsbn(String memberId, String isbn);
}
