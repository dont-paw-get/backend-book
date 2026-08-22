package com.chc.dpgb.librarian.application;

import java.util.List;
import java.util.Optional;

import com.chc.dpgb.librarian.domain.Librarian;

public interface LibrarianRepository {

    List<Librarian> findAll();

    Optional<Librarian> findById(Long librarianId);
}
