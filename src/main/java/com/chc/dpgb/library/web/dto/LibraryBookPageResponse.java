package com.chc.dpgb.library.web.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.chc.dpgb.library.LibraryBook;

public record LibraryBookPageResponse(
        List<LibraryBookSummary> books,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static LibraryBookPageResponse from(Page<LibraryBook> page) {
        return new LibraryBookPageResponse(
                page.getContent().stream().map(LibraryBookSummary::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }
}
