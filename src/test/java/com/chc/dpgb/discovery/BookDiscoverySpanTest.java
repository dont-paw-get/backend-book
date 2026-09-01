package com.chc.dpgb.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.common.exception.AladinApiException;
import com.chc.dpgb.common.exception.InvalidSearchParameterException;
import com.chc.dpgb.common.observation.RecordingObservationHandler;
import com.chc.dpgb.library.application.LibraryBookRepository;
import com.chc.dpgb.library.domain.LibraryBook;

/**
 * 도서 검색 custom span의 계약을 고정한다. 자동 계측만으로는 "이 요청이 왜 알라딘을 호출했는지"가 trace에 드러나지 않아
 * 이 span 하나를 직접 넣었으므로, span 이름과 outcome 값이 조용히 사라지지 않도록 테스트로 못 박는다.
 * <p>
 * 별도 테스트 라이브러리 없이 {@link RecordingObservationHandler}로 실제로 발생한 관측 결과만 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class BookDiscoverySpanTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final String ISBN = "9788932917245";

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private BookDiscoveryClient bookDiscoveryClient;

    private RecordingObservationHandler handler;
    private BookDiscoveryService bookDiscoveryService;

    @BeforeEach
    void setUp() {
        handler = new RecordingObservationHandler();
        bookDiscoveryService = new BookDiscoveryService(
                libraryBookRepository, bookDiscoveryClient, RecordingObservationHandler.registryWith(handler)
        );
    }

    @Test
    void 서재에_이미_있으면_outcome이_ALREADY_REGISTERED다() {
        LibraryBook book = LibraryBook.register(
                MEMBER_1, 1L, "m", "어린 왕자", "생텍쥐페리", ISBN, null, null, null, null, null, 160
        );
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.of(book));

        bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(handler.names()).containsExactly("book.discovery.search");
        assertThat(handler.lowCardinalityValues("book.discovery.outcome")).containsExactly("ALREADY_REGISTERED");
    }

    @Test
    void 알라딘에서_찾으면_outcome이_FOUND다() {
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.empty());
        when(bookDiscoveryClient.lookup(ISBN)).thenReturn(Optional.of(new ExternalBook(
                "어린 왕자", "생텍쥐페리", ISBN, "열린책들", null, 160, "https://example.com/cover.jpg"
        )));

        bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(handler.lowCardinalityValues("book.discovery.outcome")).containsExactly("FOUND");
    }

    @Test
    void 어디에도_없으면_outcome이_NOT_FOUND다() {
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.empty());
        when(bookDiscoveryClient.lookup(ISBN)).thenReturn(Optional.empty());

        bookDiscoveryService.search(MEMBER_1, ISBN);

        assertThat(handler.lowCardinalityValues("book.discovery.outcome")).containsExactly("NOT_FOUND");
    }

    @Test
    void 외부_API가_실패하면_span에_오류가_기록되고_예외는_그대로_전파된다() {
        when(libraryBookRepository.findByMemberIdAndIsbn(MEMBER_1, ISBN)).thenReturn(Optional.empty());
        when(bookDiscoveryClient.lookup(ISBN)).thenThrow(new AladinApiException());

        assertThatThrownBy(() -> bookDiscoveryService.search(MEMBER_1, ISBN))
                .isInstanceOf(AladinApiException.class);

        assertThat(handler.names()).containsExactly("book.discovery.search");
        assertThat(handler.errors()).singleElement().isInstanceOf(AladinApiException.class);
    }

    @Test
    void isbn_형식_검증에_걸리면_span을_열지_않는다() {
        assertThatThrownBy(() -> bookDiscoveryService.search(MEMBER_1, "12345"))
                .isInstanceOf(InvalidSearchParameterException.class);

        assertThat(handler.names()).isEmpty();
    }
}
