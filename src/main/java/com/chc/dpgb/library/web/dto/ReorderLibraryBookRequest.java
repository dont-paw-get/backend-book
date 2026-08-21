package com.chc.dpgb.library.web.dto;

public record ReorderLibraryBookRequest(Long beforeBookId, Long afterBookId) {
}
