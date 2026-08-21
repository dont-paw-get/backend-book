package com.chc.dpgb.discovery;

import java.time.LocalDate;

public record ExternalBook(
        String title,
        String author,
        String isbn,
        String publisher,
        LocalDate publishedDate,
        Integer totalPages,
        String coverUrl
) {
}
