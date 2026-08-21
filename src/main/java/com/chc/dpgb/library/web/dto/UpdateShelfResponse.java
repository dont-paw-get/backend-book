package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.Shelf;

public record UpdateShelfResponse(Long shelfId, String name, boolean isDefault, Instant updatedAt) {

    public static UpdateShelfResponse from(Shelf shelf) {
        return new UpdateShelfResponse(
                shelf.getShelfId(), shelf.getName(), shelf.isDefault(), shelf.getUpdatedAt()
        );
    }
}
