package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

public record CreateLibraryBookRequest(
        String title,
        String author,
        String isbn,
        String publisher,
        LocalDate publishedDate,
        Integer totalPages,
        String coverUrl,
        Long shelfId
) {
}
