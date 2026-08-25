package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.ReadingStatus;

public record UpdateLibraryBookRequest(
        String title,
        String author,
        String isbn,
        Genre genre,
        String publisher,
        LocalDate publishedDate,
        String coverUrl,
        ReadingStatus readingStatus,
        Integer totalPages
) {
}
