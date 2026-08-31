package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * {@code totalPages}가 {@code null}이면 기존 값을 지운다 — 삭제 의미이므로 {@code @NotNull}을 걸지 않는다.
 */
public record UpdateReadingProgressRequest(
        @NotNull @Min(0) Integer currentPage,
        @Min(1) Integer totalPages
) {
}
