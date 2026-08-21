package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.LibraryBook;

public record CreateLibraryBookResponse(
		Long bookId,
		Long shelfId,
		String shelfRank,
		String title,
		String author,
		int currentPage,
		double progress) {

	public static CreateLibraryBookResponse from(LibraryBook book) {
		return new CreateLibraryBookResponse(
				book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
				book.getCurrentPage(), book.progress());
	}
}
