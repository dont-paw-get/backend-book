package com.chc.dpgb.librarian.web.dto;

import com.chc.dpgb.librarian.domain.LibrarianType;

public record AcquireLibrarianRequest(LibrarianType type, String name) {
}
