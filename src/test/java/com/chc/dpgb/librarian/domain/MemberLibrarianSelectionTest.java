package com.chc.dpgb.librarian.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberLibrarianSelectionTest {

    @Test
    void 대표_사서를_선택한다() {
        MemberLibrarianSelection selection = MemberLibrarianSelection.create("member-1", 1L);

        assertThat(selection.getMemberId()).isEqualTo("member-1");
        assertThat(selection.getLibrarianId()).isEqualTo(1L);
    }

    @Test
    void 다른_사서로_변경하면_기존_선택을_덮어쓴다() {
        MemberLibrarianSelection selection = MemberLibrarianSelection.create("member-1", 1L);

        selection.select(2L);

        assertThat(selection.getLibrarianId()).isEqualTo(2L);
    }

    @Test
    void librarianId가_없으면_선택할_수_없다() {
        MemberLibrarianSelection selection = MemberLibrarianSelection.create("member-1", 1L);

        assertThatThrownBy(() -> selection.select(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
