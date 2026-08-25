package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;

public record CreateLibraryBookResponse(
        Long bookId,
        Long shelfId,
        String shelfRank,
        String title,
        String author,
        Genre genre,
        ReadingStatus readingStatus,
        int currentPage,
        Double progress
) {

    public static CreateLibraryBookResponse from(LibraryBook book) {
        return new CreateLibraryBookResponse(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
                book.getGenre(), book.getReadingStatus(), book.getCurrentPage(), book.progress()
        );
    }
}
