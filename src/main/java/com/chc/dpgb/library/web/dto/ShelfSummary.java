package com.chc.dpgb.library.web.dto;

import com.chc.dpgb.library.Shelf;

public record ShelfSummary(Long shelfId, String name, boolean isDefault, int bookCount) {

    public static ShelfSummary of(Shelf shelf, long bookCount) {
        return new ShelfSummary(
                shelf.getShelfId(), shelf.getName(), shelf.isDefault(), Math.toIntExact(bookCount)
        );
    }
}
