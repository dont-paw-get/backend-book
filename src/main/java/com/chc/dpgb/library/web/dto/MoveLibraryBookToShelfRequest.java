package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveLibraryBookToShelfRequest(@NotNull @Min(1) Long shelfId) {
}
