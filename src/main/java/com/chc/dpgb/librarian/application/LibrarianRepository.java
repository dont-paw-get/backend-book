package com.chc.dpgb.librarian.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

public interface LibrarianRepository {

    Librarian save(Librarian librarian);

    Optional<Librarian> findById(Long librarianId);

    List<Librarian> findAllOwned(UUID memberId);

    boolean existsByMemberIdAndType(UUID memberId, LibrarianType type);

    Optional<Librarian> findRepresentative(UUID memberId);
}
