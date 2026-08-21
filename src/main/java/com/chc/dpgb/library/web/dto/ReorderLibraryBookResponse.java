package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.LibraryBook;

public record ReorderLibraryBookResponse(Long bookId, String shelfRank, Instant updatedAt) {

    public static ReorderLibraryBookResponse from(LibraryBook book) {
        return new ReorderLibraryBookResponse(book.getBookId(), book.getShelfRank(), book.getUpdatedAt());
    }
}
