package com.chc.dpgb.librarian.web.dto;

import java.time.Instant;

import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

public record AcquireLibrarianResponse(
        Long librarianId, LibrarianType type, String name, int level, long experience, boolean isRepresentative,
        Instant createdAt
) {

    public static AcquireLibrarianResponse from(Librarian librarian) {
        return new AcquireLibrarianResponse(
                librarian.getLibrarianId(), librarian.getType(), librarian.getName(), librarian.getLevel(),
                librarian.getExperience(), librarian.isRepresentative(), librarian.getCreatedAt()
        );
    }
}
