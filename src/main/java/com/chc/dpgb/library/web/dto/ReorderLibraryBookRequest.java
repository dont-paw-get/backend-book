package com.chc.dpgb.library.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;

public record ReorderLibraryBookRequest(
        @Min(1) Long beforeBookId,
        @Min(1) Long afterBookId
) {

    /**
     * openapi.yaml의 {@code minProperties: 1 / maxProperties: 1}에 대응한다 — 표준 제약 애노테이션으로는 표현되지 않아
     * {@code @AssertTrue}로 옮겼다. 둘 다 비었거나 둘 다 지정된 요청을 막는다.
     */
    @AssertTrue(message = "beforeBookId 또는 afterBookId 중 정확히 하나만 지정해야 합니다.")
    public boolean isExactlyOneTargetSpecified() {
        return (beforeBookId == null) != (afterBookId == null);
    }
}
