package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateShelfRequest(@NotNull @Size(min = 1, max = 50) String name) {
}
