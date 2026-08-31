package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code sentence}/{@code memo}의 상한 2000은 DB가 아니라 이 계층이 지킨다 — V8에서 컬럼이 {@code TEXT}로 바뀌어
 * DB는 더 이상 길이를 제한하지 않는다 (ADR-0013).
 */
public record CreateScrapRequest(
        @NotNull @Size(min = 1, max = 2000) String sentence,
        @Min(1) Integer pageNumber,
        @NotNull String scrapImageUrl,
        @Size(max = 2000) String memo
) {
}
