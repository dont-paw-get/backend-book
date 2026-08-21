package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository {

    Shelf save(Shelf shelf);

    void delete(Shelf shelf);

    Optional<Shelf> findById(Long shelfId);

    Optional<Shelf> findDefaultShelf(String memberId);

    List<Shelf> findAllOwned(String memberId);
}
