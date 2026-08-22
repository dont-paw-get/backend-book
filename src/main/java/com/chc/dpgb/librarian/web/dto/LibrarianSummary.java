package com.chc.dpgb.librarian.web.dto;

import com.chc.dpgb.librarian.domain.Librarian;

public record LibrarianSummary(Long librarianId, String name, String type, String imageUrl, int evolutionStage) {

    public static LibrarianSummary from(Librarian librarian) {
        return new LibrarianSummary(
                librarian.getLibrarianId(), librarian.getName(), librarian.getType(), librarian.getImageUrl(),
                librarian.getEvolutionStage()
        );
    }
}
