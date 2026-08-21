package com.chc.dpgb.library;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface ShelfJpaRepository extends JpaRepository<Shelf, Long> {

    Optional<Shelf> findByMemberIdAndIsDefaultTrue(String memberId);

    List<Shelf> findByMemberIdOrderByIsDefaultDescCreatedAtAsc(String memberId);
}
