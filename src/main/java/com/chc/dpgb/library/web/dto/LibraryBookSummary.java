package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;

public record LibraryBookSummary(
        Long bookId,
        Long shelfId,
        String shelfRank,
        String title,
        String author,
        Genre genre,
        ReadingStatus readingStatus,
        String coverUrl,
        Double progress
) {

    public static LibraryBookSummary from(LibraryBook book) {
        return new LibraryBookSummary(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getReadingStatus(), book.getCoverUrl(), book.progress()
        );
    }
}
