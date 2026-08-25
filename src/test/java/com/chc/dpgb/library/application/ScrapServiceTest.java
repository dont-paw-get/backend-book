package com.chc.dpgb.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.common.exception.InvalidScrapDataException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;
import com.chc.dpgb.common.exception.ScrapAccessDeniedException;
import com.chc.dpgb.common.exception.ScrapNotFoundException;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Scrap;

@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private LibraryBookRepository libraryBookRepository;

    private ScrapService scrapService;

    @BeforeEach
    void setUp() {
        scrapService = new ScrapService(scrapRepository, libraryBookRepository);
    }

    @Test
    void 소유한_책에_스크랩을_생성한다() {
        LibraryBook book = book(MEMBER_1, 1L);
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Scrap result = scrapService.createScrap(MEMBER_1, 1L, "문장", 12, "https://example.com/scrap.png", "메모");

        assertThat(result.getBookId()).isEqualTo(1L);
        assertThat(result.getSentence()).isEqualTo("문장");
    }

    @Test
    void 존재하지_않는_책에_스크랩을_생성하면_404() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapService.createScrap(
                MEMBER_1, 1L, "문장", null, "https://example.com/scrap.png", null
        )).isInstanceOf(LibraryBookNotFoundException.class);
    }

    @Test
    void 다른_사용자의_책에_스크랩을_생성하면_403() {
        LibraryBook book = book(MEMBER_2, 1L);
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> scrapService.createScrap(
                MEMBER_1, 1L, "문장", null, "https://example.com/scrap.png", null
        )).isInstanceOf(LibraryBookAccessDeniedException.class);
    }

    @Test
    void sentence가_비어있으면_스크랩_생성을_거부한다() {
        LibraryBook book = book(MEMBER_1, 1L);
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> scrapService.createScrap(
                MEMBER_1, 1L, "  ", null, "https://example.com/scrap.png", null
        )).isInstanceOf(InvalidScrapDataException.class);
    }

    @Test
    void 존재하지_않는_스크랩을_조회하면_404() {
        when(scrapRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scrapService.getScrap(MEMBER_1, 1L))
                .isInstanceOf(ScrapNotFoundException.class);
    }

    @Test
    void 다른_사용자의_스크랩을_조회하면_403() {
        LibraryBook book = book(MEMBER_2, 1L);
        Scrap scrap = Scrap.create(1L, "문장", null, "https://example.com/scrap.png", null);
        when(scrapRepository.findById(1L)).thenReturn(Optional.of(scrap));
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> scrapService.getScrap(MEMBER_1, 1L))
                .isInstanceOf(ScrapAccessDeniedException.class);
    }

    @Test
    void 스크랩을_수정한다() {
        LibraryBook book = book(MEMBER_1, 1L);
        Scrap scrap = Scrap.create(1L, "원래 문장", 1, "https://example.com/scrap.png", "원래 메모");
        when(scrapRepository.findById(1L)).thenReturn(Optional.of(scrap));
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Scrap result = scrapService.updateScrap(
                MEMBER_1, 1L, "새 문장", null, "https://example.com/new-scrap.png", null
        );

        assertThat(result.getSentence()).isEqualTo("새 문장");
        assertThat(result.getPageNumber()).isNull();
        assertThat(result.getMemo()).isNull();
    }

    @Test
    void 스크랩을_삭제한다() {
        LibraryBook book = book(MEMBER_1, 1L);
        Scrap scrap = Scrap.create(1L, "문장", null, "https://example.com/scrap.png", null);
        when(scrapRepository.findById(1L)).thenReturn(Optional.of(scrap));
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scrapService.deleteScrap(MEMBER_1, 1L);

        assertThat(scrap.isDeleted()).isTrue();
        verify(scrapRepository).save(scrap);
    }

    @Test
    void softDeleteAllByBookId는_그_책의_스크랩_전체를_soft_delete한다() {
        Scrap scrap1 = Scrap.create(1L, "문장1", null, "https://example.com/scrap.png", null);
        Scrap scrap2 = Scrap.create(1L, "문장2", null, "https://example.com/scrap.png", null);
        when(scrapRepository.findAllByBookId(1L)).thenReturn(java.util.List.of(scrap1, scrap2));
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant now = Instant.now();
        scrapService.softDeleteAllByBookId(1L, now);

        assertThat(scrap1.isDeleted()).isTrue();
        assertThat(scrap2.isDeleted()).isTrue();
        verify(scrapRepository).save(scrap1);
        verify(scrapRepository).save(scrap2);
    }

    private LibraryBook book(UUID memberId, Long shelfId) {
        return LibraryBook.register(
                memberId, shelfId, "m", "제목", "저자", null, null, null, LocalDate.now(), null, null, 100
        );
    }
}
