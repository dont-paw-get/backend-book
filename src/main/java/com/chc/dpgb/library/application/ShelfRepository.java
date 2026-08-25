package com.chc.dpgb.library.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.chc.dpgb.library.domain.Shelf;

public interface ShelfRepository {

    Shelf save(Shelf shelf);

    Optional<Shelf> findById(Long shelfId);

    Optional<Shelf> findDefaultShelf(UUID memberId);

    List<Shelf> findAllOwned(UUID memberId);
}
