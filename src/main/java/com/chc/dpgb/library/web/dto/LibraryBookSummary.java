package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.LibraryBook;

public record LibraryBookSummary(
        Long bookId,
        Long shelfId,
        String shelfRank,
        String title,
        String author,
        String coverUrl,
        double progress
) {

    public static LibraryBookSummary from(LibraryBook book) {
        return new LibraryBookSummary(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
                book.getCoverUrl(), book.progress()
        );
    }
}
