package com.chc.dpgb.librarian.web.dto;

import java.time.Instant;

import com.chc.dpgb.librarian.domain.Librarian;

public record RenameLibrarianResponse(Long librarianId, String name, Instant updatedAt) {

    public static RenameLibrarianResponse from(Librarian librarian) {
        return new RenameLibrarianResponse(librarian.getLibrarianId(), librarian.getName(), librarian.getUpdatedAt());
    }
}
