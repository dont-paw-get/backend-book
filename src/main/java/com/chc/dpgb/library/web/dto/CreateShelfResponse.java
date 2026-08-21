package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.Shelf;

public record CreateShelfResponse(Long shelfId, String name, boolean isDefault, Instant createdAt) {

    public static CreateShelfResponse from(Shelf shelf) {
        return new CreateShelfResponse(
                shelf.getShelfId(), shelf.getName(), shelf.isDefault(), shelf.getCreatedAt()
        );
    }
}
