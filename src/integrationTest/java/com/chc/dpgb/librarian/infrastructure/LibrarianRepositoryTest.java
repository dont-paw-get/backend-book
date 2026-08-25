package com.chc.dpgb.librarian.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

class LibrarianRepositoryTest extends RepositoryIntegrationTestSupport {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @Autowired
    private LibrarianJpaRepository librarianRepository;

    @Test
    void 획득한_사서를_저장하고_조회할_수_있다() {
        Librarian saved = librarianRepository.saveAndFlush(
                Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비")
        );

        Optional<Librarian> found = librarianRepository.findById(saved.getLibrarianId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("나비");
        assertThat(found.get().getLevel()).isEqualTo(1);
        assertThat(found.get().getExperience()).isZero();
    }

    @Test
    void 같은_회원이_같은_타입을_두_마리_보유할_수_없다() {
        librarianRepository.saveAndFlush(Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"));
        Librarian duplicate = Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비2");

        assertThatThrownBy(() -> librarianRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_회원이_다른_타입은_각각_보유할_수_있다() {
        librarianRepository.saveAndFlush(Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"));
        Librarian other = Librarian.acquire(MEMBER_1, LibrarianType.SHOEBILL, "부엉");

        assertThat(librarianRepository.existsByMemberIdAndType(MEMBER_1, LibrarianType.SHOEBILL)).isFalse();
        Librarian saved = librarianRepository.saveAndFlush(other);

        assertThat(saved.getLibrarianId()).isNotNull();
    }

    @Test
    void 회원당_대표_사서는_한_마리만_가능하다() {
        Librarian first = Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        first.markAsRepresentative();
        librarianRepository.saveAndFlush(first);

        Librarian second = Librarian.acquire(MEMBER_1, LibrarianType.SHOEBILL, "부엉");
        second.markAsRepresentative();

        assertThatThrownBy(() -> librarianRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 회원의_대표_사서를_조회한다() {
        Librarian representative = Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비");
        representative.markAsRepresentative();
        librarianRepository.saveAndFlush(representative);
        librarianRepository.saveAndFlush(Librarian.acquire(MEMBER_1, LibrarianType.SHOEBILL, "부엉"));

        Optional<Librarian> found = librarianRepository.findByMemberIdAndIsRepresentativeTrue(MEMBER_1);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("나비");
    }

    @Test
    void 다른_회원끼리는_같은_타입을_각각_보유할_수_있다() {
        librarianRepository.saveAndFlush(Librarian.acquire(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"));
        Librarian other = Librarian.acquire(MEMBER_2, LibrarianType.RUSSIAN_BLUE, "나비2");

        Librarian saved = librarianRepository.saveAndFlush(other);

        assertThat(saved.getLibrarianId()).isNotNull();
    }
}
