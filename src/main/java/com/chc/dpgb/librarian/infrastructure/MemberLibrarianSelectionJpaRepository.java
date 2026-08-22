package com.chc.dpgb.librarian.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

interface MemberLibrarianSelectionJpaRepository extends JpaRepository<MemberLibrarianSelection, Long> {

    Optional<MemberLibrarianSelection> findByMemberId(String memberId);
}
