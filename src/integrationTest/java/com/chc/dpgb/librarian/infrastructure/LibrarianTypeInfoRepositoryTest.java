package com.chc.dpgb.librarian.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.chc.dpgb.RepositoryIntegrationTestSupport;
import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

class LibrarianTypeInfoRepositoryTest extends RepositoryIntegrationTestSupport {

    @Autowired
    private LibrarianTypeInfoJpaRepository librarianTypeInfoRepository;

    @Test
    void 시드된_사서_타입_카탈로그를_조회할_수_있다() {
        List<LibrarianTypeInfo> types = librarianTypeInfoRepository.findAll();

        assertThat(types).extracting(LibrarianTypeInfo::getType)
                .containsExactlyInAnyOrder(LibrarianType.RUSSIAN_BLUE, LibrarianType.SHOEBILL);
    }
}
