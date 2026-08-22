package com.chc.dpgb.librarian.application;

import java.time.Instant;

import com.chc.dpgb.librarian.domain.Librarian;

public record SelectedLibrarian(Librarian librarian, Instant selectedAt) {
}
