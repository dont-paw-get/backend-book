package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.LibraryBook;

public record MoveLibraryBookToShelfResponse(
        Long bookId, Long shelfId, String shelfRank, Instant updatedAt
) {

    public static MoveLibraryBookToShelfResponse from(LibraryBook book) {
        return new MoveLibraryBookToShelfResponse(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getUpdatedAt()
        );
    }
}
