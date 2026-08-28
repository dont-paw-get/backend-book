package com.chc.dpgb.discovery;

import com.chc.dpgb.library.domain.LibraryBook;

public record BookSearchResult(LibraryBook libraryBook, ExternalBook book) {

    public static BookSearchResult alreadyRegistered(LibraryBook libraryBook) {
        return new BookSearchResult(libraryBook, null);
    }

    public static BookSearchResult found(ExternalBook book) {
        return new BookSearchResult(null, book);
    }

    public static BookSearchResult notFound() {
        return new BookSearchResult(null, null);
    }

    public boolean alreadyRegistered() {
        return libraryBook != null;
    }
}
