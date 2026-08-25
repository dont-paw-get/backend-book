package com.chc.dpgb.librarian.web.dto;

import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

public record LibrarianTypeSummary(LibrarianType type, String imageUrl, String clickedImageUrl) {

    public static LibrarianTypeSummary from(LibrarianTypeInfo typeInfo) {
        return new LibrarianTypeSummary(typeInfo.getType(), typeInfo.getImageUrl(), typeInfo.getClickedImageUrl());
    }
}
