package com.chc.dpgb.librarian.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.chc.dpgb.librarian.application.MemberLibrarianSelectionRepository;
import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

@Repository
class MemberLibrarianSelectionRepositoryJpaAdapter implements MemberLibrarianSelectionRepository {

    private final MemberLibrarianSelectionJpaRepository jpaRepository;

    MemberLibrarianSelectionRepositoryJpaAdapter(MemberLibrarianSelectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MemberLibrarianSelection save(MemberLibrarianSelection selection) {
        return jpaRepository.saveAndFlush(selection);
    }

    @Override
    public Optional<MemberLibrarianSelection> findByMemberId(String memberId) {
        return jpaRepository.findByMemberId(memberId);
    }
}
