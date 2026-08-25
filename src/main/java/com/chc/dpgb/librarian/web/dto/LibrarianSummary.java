package com.chc.dpgb.librarian.web.dto;

import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

public record LibrarianSummary(
        Long librarianId, LibrarianType type, String name, int level, long experience, boolean isRepresentative
) {

    public static LibrarianSummary from(Librarian librarian) {
        return new LibrarianSummary(
                librarian.getLibrarianId(), librarian.getType(), librarian.getName(), librarian.getLevel(),
                librarian.getExperience(), librarian.isRepresentative()
        );
    }
}
