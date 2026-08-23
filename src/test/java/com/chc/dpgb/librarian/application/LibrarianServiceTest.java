package com.chc.dpgb.librarian.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.librarian.domain.Librarian;

@ExtendWith(MockitoExtension.class)
class LibrarianServiceTest {

    @Mock
    private LibrarianRepository librarianRepository;

    private LibrarianService librarianService;

    @BeforeEach
    void setUp() {
        librarianService = new LibrarianService(librarianRepository);
    }

    @Test
    void 사서_마스터_목록을_조회한다() {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        when(librarianRepository.findAll()).thenReturn(List.of(cat));

        List<Librarian> result = librarianService.getLibrarians();

        assertThat(result).containsExactly(cat);
    }
}
