package com.chc.dpgb.librarian.web.dto;

import java.util.List;

import com.chc.dpgb.librarian.domain.Librarian;

public record LibrarianListResponse(List<LibrarianSummary> librarians) {

    public static LibrarianListResponse from(List<Librarian> librarians) {
        return new LibrarianListResponse(librarians.stream().map(LibrarianSummary::from).toList());
    }
}
