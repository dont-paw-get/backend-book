package com.chc.dpgb.librarian.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.LibrarianNotSelectedException;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

@ExtendWith(MockitoExtension.class)
class LibrarianServiceTest {

    @Mock
    private LibrarianRepository librarianRepository;

    @Mock
    private MemberLibrarianSelectionRepository selectionRepository;

    private LibrarianService librarianService;

    @BeforeEach
    void setUp() {
        librarianService = new LibrarianService(librarianRepository, selectionRepository);
    }

    @Test
    void 사서_마스터_목록을_조회한다() {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        when(librarianRepository.findAll()).thenReturn(List.of(cat));

        List<Librarian> result = librarianService.getLibrarians();

        assertThat(result).containsExactly(cat);
    }

    @Test
    void 대표_사서를_선택하지_않았으면_404() {
        when(selectionRepository.findByMemberId("member-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> librarianService.getMyLibrarian("member-1"))
                .isInstanceOf(LibrarianNotSelectedException.class);
    }

    @Test
    void 선택한_대표_사서를_조회한다() {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        MemberLibrarianSelection selection = MemberLibrarianSelection.create("member-1", 1L);
        when(selectionRepository.findByMemberId("member-1")).thenReturn(Optional.of(selection));
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(cat));

        SelectedLibrarian result = librarianService.getMyLibrarian("member-1");

        assertThat(result.librarian()).isEqualTo(cat);
        assertThat(result.selectedAt()).isEqualTo(selection.getSelectedAt());
    }

    @Test
    void 존재하지_않는_사서를_선택하면_404() {
        when(librarianRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> librarianService.selectMyLibrarian("member-1", 99L))
                .isInstanceOf(LibrarianNotFoundException.class);
    }

    @Test
    void 처음_선택하면_새로_생성한다() {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(selectionRepository.findByMemberId("member-1")).thenReturn(Optional.empty());
        when(selectionRepository.save(any(MemberLibrarianSelection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SelectedLibrarian result = librarianService.selectMyLibrarian("member-1", 1L);

        assertThat(result.librarian()).isEqualTo(cat);
    }

    @Test
    void 이미_선택한_회원은_기존_선택을_덮어쓴다() {
        Librarian bird = new Librarian(2L, "슈빌", "BIRD", "https://example.com/librarians/bird-1.png", 1);
        MemberLibrarianSelection existing = MemberLibrarianSelection.create("member-1", 1L);
        when(librarianRepository.findById(2L)).thenReturn(Optional.of(bird));
        when(selectionRepository.findByMemberId("member-1")).thenReturn(Optional.of(existing));
        when(selectionRepository.save(any(MemberLibrarianSelection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SelectedLibrarian result = librarianService.selectMyLibrarian("member-1", 2L);

        assertThat(result.librarian()).isEqualTo(bird);
        assertThat(existing.getLibrarianId()).isEqualTo(2L);
    }

    @Test
    void 동시_최초_선택으로_유일성_제약을_위반하면_다시_조회해_덮어쓴다() {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        MemberLibrarianSelection createdByRace = MemberLibrarianSelection.create("member-1", 2L);
        when(librarianRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(selectionRepository.findByMemberId("member-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdByRace));
        when(selectionRepository.save(any(MemberLibrarianSelection.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SelectedLibrarian result = librarianService.selectMyLibrarian("member-1", 1L);

        assertThat(result.librarian()).isEqualTo(cat);
        assertThat(createdByRace.getLibrarianId()).isEqualTo(1L);
    }
}
