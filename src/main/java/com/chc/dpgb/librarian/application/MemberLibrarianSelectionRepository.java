package com.chc.dpgb.librarian.application;

import java.util.Optional;

import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

public interface MemberLibrarianSelectionRepository {

    MemberLibrarianSelection save(MemberLibrarianSelection selection);

    Optional<MemberLibrarianSelection> findByMemberId(String memberId);
}
