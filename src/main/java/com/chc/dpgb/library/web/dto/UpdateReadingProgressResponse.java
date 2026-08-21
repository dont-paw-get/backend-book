package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.LibraryBook;

public record UpdateReadingProgressResponse(
        Long bookId, int currentPage, int totalPages, double progress, Instant updatedAt
) {

    public static UpdateReadingProgressResponse from(LibraryBook book) {
        return new UpdateReadingProgressResponse(
                book.getBookId(), book.getCurrentPage(), book.getTotalPages(), book.progress(),
                book.getUpdatedAt()
        );
    }
}
