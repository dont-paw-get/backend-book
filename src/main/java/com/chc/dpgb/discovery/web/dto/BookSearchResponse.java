package com.chc.dpgb.discovery.web.dto;

import com.chc.dpgb.discovery.BookSearchResult;
import com.chc.dpgb.discovery.ExternalBook;
import com.chc.dpgb.library.web.dto.LibraryBookDetailResponse;

public record BookSearchResponse(boolean alreadyRegistered, LibraryBookDetailResponse libraryBook, ExternalBook book) {

    public static BookSearchResponse from(BookSearchResult result) {
        if (result.alreadyRegistered()) {
            return new BookSearchResponse(true, LibraryBookDetailResponse.from(result.libraryBook()), null);
        }
        return new BookSearchResponse(false, null, result.book());
    }
}
