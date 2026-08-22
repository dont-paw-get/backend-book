package com.chc.dpgb.librarian.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

class MemberLibrarianSelectionRepositoryTest extends RepositoryIntegrationTestSupport {

    @Autowired
    private MemberLibrarianSelectionJpaRepository selectionRepository;

    @Test
    void 회원의_대표_사서_선택을_저장하고_조회할_수_있다() {
        selectionRepository.saveAndFlush(MemberLibrarianSelection.create("member-1", 1L));

        Optional<MemberLibrarianSelection> found = selectionRepository.findByMemberId("member-1");

        assertThat(found).isPresent();
        assertThat(found.get().getLibrarianId()).isEqualTo(1L);
    }

    @Test
    void 선택하지_않은_회원은_조회되지_않는다() {
        Optional<MemberLibrarianSelection> found = selectionRepository.findByMemberId("member-1");

        assertThat(found).isEmpty();
    }

    @Test
    void 같은_회원이_두_개의_선택을_가질_수_없다() {
        selectionRepository.saveAndFlush(MemberLibrarianSelection.create("member-1", 1L));
        MemberLibrarianSelection duplicate = MemberLibrarianSelection.create("member-1", 2L);

        assertThatThrownBy(() -> selectionRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_회원은_각자_대표_사서를_선택할_수_있다() {
        selectionRepository.saveAndFlush(MemberLibrarianSelection.create("member-1", 1L));
        MemberLibrarianSelection other = MemberLibrarianSelection.create("member-2", 1L);

        MemberLibrarianSelection saved = selectionRepository.saveAndFlush(other);

        assertThat(saved.getId()).isNotNull();
    }
}
