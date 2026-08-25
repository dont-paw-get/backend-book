package com.chc.dpgb.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.library.domain.Shelf;

interface ShelfJpaRepository extends JpaRepository<Shelf, Long> {

    Optional<Shelf> findByMemberIdAndIsDefaultTrue(UUID memberId);

    List<Shelf> findByMemberIdOrderByIsDefaultDescCreatedAtAsc(UUID memberId);
}
