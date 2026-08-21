package com.chc.dpgb.library.application;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.common.exception.DefaultShelfCannotBeDeletedException;
import com.chc.dpgb.common.exception.InvalidShelfDataException;
import com.chc.dpgb.common.exception.ShelfAccessDeniedException;
import com.chc.dpgb.common.exception.ShelfNotFoundException;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Shelf;
import com.chc.dpgb.library.domain.ShelfRank;

@Service
public class ShelfService {

    private static final String DEFAULT_SHELF_NAME = "기본 책장";

    private final ShelfRepository shelfRepository;
    private final LibraryBookRepository libraryBookRepository;

    ShelfService(ShelfRepository shelfRepository, LibraryBookRepository libraryBookRepository) {
        this.shelfRepository = shelfRepository;
        this.libraryBookRepository = libraryBookRepository;
    }

    @Transactional
    public Shelf getOrCreateDefaultShelf(String memberId) {
        Optional<Shelf> existing = shelfRepository.findDefaultShelf(memberId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return shelfRepository.save(Shelf.create(memberId, DEFAULT_SHELF_NAME, true));
        } catch (DataIntegrityViolationException e) {
            return shelfRepository.findDefaultShelf(memberId).orElseThrow(() -> e);
        }
    }

    @Transactional
    public Shelf createShelf(String memberId, String name) {
        try {
            return shelfRepository.save(Shelf.create(memberId, name, false));
        } catch (IllegalArgumentException e) {
            throw new InvalidShelfDataException(e.getMessage());
        }
    }

    public List<Shelf> getShelves(String memberId) {
        getOrCreateDefaultShelf(memberId);
        return shelfRepository.findAllOwned(memberId);
    }

    @Transactional(readOnly = true)
    public long bookCount(Long shelfId) {
        return libraryBookRepository.countByShelfId(shelfId);
    }

    @Transactional
    public Shelf updateShelf(String memberId, Long shelfId, String name) {
        Shelf shelf = getOwnedShelf(memberId, shelfId);
        try {
            shelf.rename(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidShelfDataException(e.getMessage());
        }
        return shelfRepository.save(shelf);
    }

    @Transactional
    public void deleteShelf(String memberId, Long shelfId) {
        Shelf shelf = getOwnedShelf(memberId, shelfId);
        if (shelf.isDefault()) {
            throw new DefaultShelfCannotBeDeletedException();
        }
        Shelf defaultShelf = getOrCreateDefaultShelf(memberId);
        for (LibraryBook book : libraryBookRepository.findShelfOrderedByRank(shelf.getShelfId())) {
            String newRank = libraryBookRepository.findLastRanked(defaultShelf.getShelfId())
                    .map(last -> ShelfRank.after(last.getShelfRank()))
                    .orElseGet(ShelfRank::initial);
            book.changeShelfId(defaultShelf.getShelfId(), newRank);
            libraryBookRepository.save(book);
        }
        shelfRepository.delete(shelf);
    }

    @Transactional(readOnly = true)
    public Shelf getOwnedShelf(String memberId, Long shelfId) {
        Shelf shelf = shelfRepository.findById(shelfId).orElseThrow(ShelfNotFoundException::new);
        if (!shelf.getMemberId().equals(memberId)) {
            throw new ShelfAccessDeniedException();
        }
        return shelf;
    }
}
