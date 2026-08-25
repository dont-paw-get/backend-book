package com.chc.dpgb.librarian.web.dto;

import java.time.Instant;

import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

public record RepresentativeLibrarianResponse(
        Long librarianId, LibrarianType type, String name, int level, Instant updatedAt
) {

    public static RepresentativeLibrarianResponse from(Librarian librarian) {
        return new RepresentativeLibrarianResponse(
                librarian.getLibrarianId(), librarian.getType(), librarian.getName(), librarian.getLevel(),
                librarian.getUpdatedAt()
        );
    }
}
