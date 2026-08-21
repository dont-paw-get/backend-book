package com.chc.dpgb.library.application;

import java.util.List;
import java.util.Optional;

import com.chc.dpgb.library.domain.Shelf;

public interface ShelfRepository {

    Shelf save(Shelf shelf);

    void delete(Shelf shelf);

    Optional<Shelf> findById(Long shelfId);

    Optional<Shelf> findDefaultShelf(String memberId);

    List<Shelf> findAllOwned(String memberId);
}
