package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 네 필드를 항상 모두 포함하는 전체 교체 방식이다. {@code pageNumber}/{@code memo}는 {@code null}이면 삭제를 뜻하므로
 * {@code @NotNull}을 걸지 않는다.
 */
public record UpdateScrapRequest(
        @NotNull @Size(min = 1, max = 2000) String sentence,
        @Min(1) Integer pageNumber,
        @NotNull String scrapImageUrl,
        @Size(max = 2000) String memo
) {
}
