package com.chc.dpgb.library.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.library.domain.Shelf;

interface ShelfJpaRepository extends JpaRepository<Shelf, Long> {

    Optional<Shelf> findByMemberIdAndIsDefaultTrue(String memberId);

    List<Shelf> findByMemberIdOrderByIsDefaultDescCreatedAtAsc(String memberId);
}
