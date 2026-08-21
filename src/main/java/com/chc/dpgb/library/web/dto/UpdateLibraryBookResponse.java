package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.LibraryBook;

public record UpdateLibraryBookResponse(
        Long bookId,
        String shelfRank,
        String title,
        String coverUrl,
        int totalPages,
        Instant updatedAt
) {

    public static UpdateLibraryBookResponse from(LibraryBook book) {
        return new UpdateLibraryBookResponse(
                book.getBookId(), book.getShelfRank(), book.getTitle(), book.getCoverUrl(),
                book.getTotalPages(), book.getUpdatedAt()
        );
    }
}
