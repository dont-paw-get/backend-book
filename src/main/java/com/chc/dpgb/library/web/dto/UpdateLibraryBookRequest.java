package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.ReadingStatus;

/**
 * 9개 필드를 항상 모두 포함하는 전체 교체 방식이다(ADR-0006). {@code isbn}/{@code publisher}/
 * {@code publishedDate}/{@code coverUrl}/{@code totalPages}는 {@code null}이면 삭제를 뜻하므로 {@code @NotNull}을
 * 걸지 않는다 — "키가 없는 것"과 "null"의 구분은 Bean Validation이 아니라 계약(문서)이 담당한다.
 */
public record UpdateLibraryBookRequest(
        @NotNull @Size(min = 1, max = 200) String title,
        @NotNull @Size(min = 1, max = 100) String author,
        @Pattern(regexp = "^(?:[0-9]{10}|[0-9]{13})$") String isbn,
        @NotNull Genre genre,
        @Size(min = 1, max = 100) String publisher,
        LocalDate publishedDate,
        String coverUrl,
        @NotNull ReadingStatus readingStatus,
        @Min(1) Integer totalPages
) {
}
