package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.ReadingStatus;

public record CreateLibraryBookRequest(
        String title,
        String author,
        String isbn,
        Genre genre,
        String publisher,
        LocalDate publishedDate,
        Integer totalPages,
        String coverUrl,
        ReadingStatus readingStatus,
        Long shelfId
) {
}
