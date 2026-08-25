package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;

public record UpdateLibraryBookResponse(
        Long bookId,
        String shelfRank,
        String title,
        Genre genre,
        String coverUrl,
        ReadingStatus readingStatus,
        Integer totalPages,
        Instant updatedAt
) {

    public static UpdateLibraryBookResponse from(LibraryBook book) {
        return new UpdateLibraryBookResponse(
                book.getBookId(), book.getShelfRank(), book.getTitle(), book.getGenre(), book.getCoverUrl(),
                book.getReadingStatus(), book.getTotalPages(), book.getUpdatedAt()
        );
    }
}
