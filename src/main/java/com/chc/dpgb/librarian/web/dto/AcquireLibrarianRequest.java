package com.chc.dpgb.librarian.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.chc.dpgb.librarian.domain.LibrarianType;

public record AcquireLibrarianRequest(
        @NotNull LibrarianType type,
        @NotNull @Size(min = 1, max = 50) String name
) {
}
