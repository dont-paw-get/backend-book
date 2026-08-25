package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.ReadingStatus;

public record LibraryBookDetailResponse(
        Long bookId,
        Long shelfId,
        String shelfRank,
        String title,
        String author,
        String isbn,
        Genre genre,
        String publisher,
        Integer totalPages,
        String coverUrl,
        ReadingStatus readingStatus,
        int currentPage,
        Double progress
) {

    public static LibraryBookDetailResponse from(LibraryBook book) {
        return new LibraryBookDetailResponse(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
                book.getIsbn(), book.getGenre(), book.getPublisher(), book.getTotalPages(), book.getCoverUrl(),
                book.getReadingStatus(), book.getCurrentPage(), book.progress()
        );
    }
}
