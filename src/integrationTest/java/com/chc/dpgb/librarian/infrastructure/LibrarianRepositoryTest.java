package com.chc.dpgb.librarian.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.librarian.domain.Librarian;

class LibrarianRepositoryTest extends RepositoryIntegrationTestSupport {

    @Autowired
    private LibrarianJpaRepository librarianRepository;

    @Test
    void 시드된_사서_마스터_목록을_조회할_수_있다() {
        List<Librarian> librarians = librarianRepository.findAll();

        assertThat(librarians).extracting(Librarian::getName).contains("러시안블루", "슈빌");
    }

    @Test
    void id로_사서를_조회할_수_있다() {
        Optional<Librarian> found = librarianRepository.findById(1L);

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo("CAT");
    }

    @Test
    void 존재하지_않는_id는_조회되지_않는다() {
        Optional<Librarian> found = librarianRepository.findById(9999L);

        assertThat(found).isEmpty();
    }
}
