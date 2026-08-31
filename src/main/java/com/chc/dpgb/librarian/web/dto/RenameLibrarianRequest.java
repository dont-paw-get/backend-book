package com.chc.dpgb.librarian.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RenameLibrarianRequest(@NotNull @Size(min = 1, max = 50) String name) {
}
