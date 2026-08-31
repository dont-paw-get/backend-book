package com.chc.dpgb.library.web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.ReadingStatus;

/**
 * 제약은 {@code docs/api/openapi.yaml}의 {@code CreateLibraryBookRequest}와 1:1로 대응한다 (ADR-0013).
 * {@code coverUrl}의 {@code format: uri}는 표준 제약으로 옮기지 않았다 — 자세한 사유는 ADR-0013 참고.
 */
public record CreateLibraryBookRequest(
        @NotNull @Size(min = 1, max = 200) String title,
        @NotNull @Size(min = 1, max = 100) String author,
        @Pattern(regexp = "^(?:[0-9]{10}|[0-9]{13})$") String isbn,
        Genre genre,
        @Size(max = 100) String publisher,
        LocalDate publishedDate,
        @Min(1) Integer totalPages,
        String coverUrl,
        ReadingStatus readingStatus,
        @Min(1) Long shelfId
) {
}
