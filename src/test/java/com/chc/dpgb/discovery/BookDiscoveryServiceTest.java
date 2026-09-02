package com.chc.dpgb.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

    private static ExternalBook book(String isbn) {
        return new ExternalBook("제목 " + isbn, "저자", isbn, "출판사", null, null, null);
    }

    @Test
    void 제목이나_저자가_비어_있으면_400() {
        assertThatThrownBy(() -> bookDiscoveryService.searchByTitleAndAuthor(null, "생텍쥐페리"))
                .isInstanceOf(InvalidSearchParameterException.class);
        assertThatThrownBy(() -> bookDiscoveryService.searchByTitleAndAuthor("어린 왕자", "  "))
                .isInstanceOf(InvalidSearchParameterException.class);
        verifyNoInteractions(bookDiscoveryClient);
    }

    @Test
    void 제목_저자_교집합의_제목검색_순서상_최상단_1권을_반환한다() {
        ExternalBook a = book("9788900000001");
        ExternalBook b = book("9788900000002");
        ExternalBook c = book("9788900000003");
        // 제목 검색: [a, b, c] / 저자 검색: [c, b] → 교집합 {b, c}, 제목 순서상 b가 먼저
        when(bookDiscoveryClient.searchByTitle("어린 왕자")).thenReturn(List.of(a, b, c));
        when(bookDiscoveryClient.searchByAuthor("생텍쥐페리")).thenReturn(List.of(c, b));

        Optional<ExternalBook> result = bookDiscoveryService.searchByTitleAndAuthor("어린 왕자", "생텍쥐페리");

        assertThat(result).contains(b);
    }

    @Test
    void 교집합이_비면_빈_결과를_반환한다() {
        when(bookDiscoveryClient.searchByTitle("어린 왕자")).thenReturn(List.of(book("9788900000001")));
        when(bookDiscoveryClient.searchByAuthor("생텍쥐페리")).thenReturn(List.of(book("9788900000009")));

        assertThat(bookDiscoveryService.searchByTitleAndAuthor("어린 왕자", "생텍쥐페리")).isEmpty();
    }

    @Test
    void 한쪽_검색_결과가_없으면_빈_결과를_반환한다() {
        when(bookDiscoveryClient.searchByTitle("어린 왕자")).thenReturn(List.of(book("9788900000001")));
        when(bookDiscoveryClient.searchByAuthor("생텍쥐페리")).thenReturn(List.of());

        assertThat(bookDiscoveryService.searchByTitleAndAuthor("어린 왕자", "생텍쥐페리")).isEmpty();
    }

    @Test
    void isbn이_없는_후보는_교집합_대상에서_제외한다() {
        ExternalBook noIsbn = new ExternalBook("어린 왕자", "저자", null, null, null, null, null);
        when(bookDiscoveryClient.searchByTitle("어린 왕자")).thenReturn(List.of(noIsbn));
        when(bookDiscoveryClient.searchByAuthor("생텍쥐페리")).thenReturn(List.of(noIsbn));

        assertThat(bookDiscoveryService.searchByTitleAndAuthor("어린 왕자", "생텍쥐페리")).isEmpty();
    }
}
