package com.chc.dpgb.librarian.web.dto;

import java.util.List;

import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

public record LibrarianTypeListResponse(List<LibrarianTypeSummary> types) {

    public static LibrarianTypeListResponse from(List<LibrarianTypeInfo> typeInfos) {
        return new LibrarianTypeListResponse(typeInfos.stream().map(LibrarianTypeSummary::from).toList());
    }
}
