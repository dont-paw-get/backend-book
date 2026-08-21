package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.domain.LibraryBook;

public record LibraryBookDetailResponse(
        Long bookId,
        Long shelfId,
        String shelfRank,
        String title,
        String author,
        String isbn,
        String publisher,
        int totalPages,
        String coverUrl,
        int currentPage,
        double progress
) {

    public static LibraryBookDetailResponse from(LibraryBook book) {
        return new LibraryBookDetailResponse(
                book.getBookId(), book.getShelfId(), book.getShelfRank(), book.getTitle(), book.getAuthor(),
                book.getIsbn(), book.getPublisher(), book.getTotalPages(), book.getCoverUrl(),
                book.getCurrentPage(), book.progress()
        );
    }
}
