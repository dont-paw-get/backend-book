package com.chc.dpgb.library.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.chc.dpgb.library.application.ShelfRepository;
import com.chc.dpgb.library.domain.Shelf;

@Repository
class ShelfRepositoryJpaAdapter implements ShelfRepository {

    private final ShelfJpaRepository jpaRepository;

    ShelfRepositoryJpaAdapter(ShelfJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Shelf save(Shelf shelf) {
        return jpaRepository.saveAndFlush(shelf);
    }

    @Override
    public void delete(Shelf shelf) {
        jpaRepository.delete(shelf);
    }

    @Override
    public Optional<Shelf> findById(Long shelfId) {
        return jpaRepository.findById(shelfId);
    }

    @Override
    public Optional<Shelf> findDefaultShelf(String memberId) {
        return jpaRepository.findByMemberIdAndIsDefaultTrue(memberId);
    }

    @Override
    public List<Shelf> findAllOwned(String memberId) {
        return jpaRepository.findByMemberIdOrderByIsDefaultDescCreatedAtAsc(memberId);
    }
}
