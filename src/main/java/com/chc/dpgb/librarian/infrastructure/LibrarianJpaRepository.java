package com.chc.dpgb.librarian.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

interface LibrarianJpaRepository extends JpaRepository<Librarian, Long> {

    List<Librarian> findByMemberIdOrderByCreatedAtAsc(UUID memberId);

    boolean existsByMemberIdAndType(UUID memberId, LibrarianType type);

    Optional<Librarian> findByMemberIdAndIsRepresentativeTrue(UUID memberId);
}
