package com.chc.dpgb.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.common.exception.InvalidSearchParameterException;
import com.chc.dpgb.library.application.LibraryBookRepository;
import com.chc.dpgb.library.domain.LibraryBook;

@ExtendWith(MockitoExtension.class)
class BookDiscoveryServiceTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final String ISBN = "9788932917245";

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private BookDiscoveryClient bookDiscoveryClient;

    private BookDiscoveryService bookDiscoveryService;

    @BeforeEach
    void setUp() {
        bookDiscoveryService = new BookDiscoveryService(
                libraryBookRepository, bookDiscoveryClient, ObservationRegistry.NOOP
        );
    }

    @Test
    void isbn이_없거나_형식이_틀리면_400() {
        assertThatThrownBy(() -> bookDiscoveryService.search(MEMBER_1, null))
                .isInstanceOf(InvalidSearchParameterException.class);
        assertThatThrownBy(() -> bookDiscoveryService.search(MEMBER_1, "  "))
                .isInstanceOf(InvalidSearchParameterException.class);
        assertThatThrownBy(() -> bookDiscoveryService.search(MEMBER_1, "12345"))
                .isInstanceOf(InvalidSearchParameterException.class);
    }

    @Test
    void 이미_서재에_등록되어_있으면_알라딘을_호출하지_않고_저장된_데이터를_반환한다() {
        LibraryBook book = LibraryBook.register(
                MEMBER_1, 1L, "m", "어린 왕자", "생텍쥐페리", ISBN, null, null, null, null, null, 160
        );
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.of(book));

        BookSearchResult result = bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(result.alreadyRegistered()).isTrue();
        assertThat(result.libraryBook()).isEqualTo(book);
        verifyNoInteractions(bookDiscoveryClient);
    }

    @Test
    void 등록되어_있지_않으면_알라딘에서_조회한다() {
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.empty());
        ExternalBook externalBook = new ExternalBook(
                "어린 왕자", "생텍쥐페리", ISBN, "열린책들", null, 160, "https://example.com/cover.jpg"
        );
        when(bookDiscoveryClient.lookup(ISBN)).thenReturn(Optional.of(externalBook));

        BookSearchResult result = bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(result.alreadyRegistered()).isFalse();
        assertThat(result.book()).isEqualTo(externalBook);
    }

    @Test
    void 등록되지도_알라딘에도_없으면_둘_다_없는_결과를_반환한다() {
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.empty());
        when(bookDiscoveryClient.lookup(ISBN)).thenReturn(Optional.empty());

        BookSearchResult result = bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(result.alreadyRegistered()).isFalse();
        assertThat(result.book()).isNull();
    }
}
