package com.chc.dpgb.librarian.web.dto;

import java.time.Instant;

import com.chc.dpgb.librarian.application.SelectedLibrarian;

public record MyLibrarianResponse(
        Long librarianId, String name, String type, String imageUrl, int evolutionStage, Instant selectedAt
) {

    public static MyLibrarianResponse from(SelectedLibrarian selected) {
        return new MyLibrarianResponse(
                selected.librarian().getLibrarianId(), selected.librarian().getName(),
                selected.librarian().getType(), selected.librarian().getImageUrl(),
                selected.librarian().getEvolutionStage(), selected.selectedAt()
        );
    }
}
