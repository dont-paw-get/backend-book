package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

public record UpdateLibraryBookRequest(
		String title,
		String author,
		String isbn,
		String publisher,
		LocalDate publishedDate,
		String coverUrl,
		Integer totalPages) {
}
