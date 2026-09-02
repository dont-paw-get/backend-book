package com.chc.dpgb.discovery.web.dto;

import java.util.Optional;

import com.chc.dpgb.discovery.ExternalBook;

public record TitleAuthorBookSearchResponse(ExternalBook book) {

    public static TitleAuthorBookSearchResponse from(Optional<ExternalBook> book) {
        return new TitleAuthorBookSearchResponse(book.orElse(null));
    }
}
