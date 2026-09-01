package com.chc.dpgb.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.common.observation.RecordingObservationHandler;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Shelf;

/**
 * 책장 재정렬(rebalance) custom span의 계약을 고정한다. rebalance는 드물게 일어나지만 한 요청 안에서 책장 전체를 다시 저장하므로,
 * 자동 계측만 보면 평범한 등록 요청이 갑자기 다량의 UPDATE를 낸 것처럼 보인다. 그 이유를 설명하는 span이라 이름과 속성을 못 박는다.
 */
@ExtendWith(MockitoExtension.class)
class ShelfRebalanceSpanTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final Long SHELF_ID = 1L;

    /**
     * {@code ShelfRank.between}은 키 길이가 128자에 닿으면 {@code ShelfRankExhaustedException}을 던진다.
     * 알파벳의 마지막 문자로만 채워진 이 랭크 뒤에 새 랭크를 만들려 하면 그 한계에 걸린다.
     */
    private static final String EXHAUSTED_RANK = "z".repeat(128);

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private ShelfService shelfService;

    @Mock
    private ScrapService scrapService;

    private RecordingObservationHandler handler;
    private LibraryBookService libraryBookService;

    @BeforeEach
    void setUp() {
        handler = new RecordingObservationHandler();
        libraryBookService = new LibraryBookService(
                libraryBookRepository,
                shelfRepository,
                shelfService,
                scrapService,
                RecordingObservationHandler.registryWith(handler)
        );
    }

    @Test
    void 랭크_키_공간이_소진되면_재정렬_span을_남기고_등록을_이어간다() {
        Shelf shelf = Shelf.create(MEMBER_1, "기본 책장", true);
        when(shelfService.getOrCreateDefaultShelf(MEMBER_1)).thenReturn(shelf);
        LibraryBook last = LibraryBook.register(
                MEMBER_1, SHELF_ID, EXHAUSTED_RANK, "이전 책", "저자", null, null, null, null, null, null, 100
        );
        // 재정렬이 이 책의 랭크를 정상 값으로 되돌리므로, 두 번째 조회에서는 새 랭크 계산이 성공한다.
        when(libraryBookRepository.findLastRanked(shelf.getShelfId())).thenReturn(Optional.of(last));
        when(libraryBookRepository.findShelfOrderedByRank(shelf.getShelfId())).thenReturn(List.of(last));
        when(libraryBookRepository.save(any(LibraryBook.class))).thenAnswer(i -> i.getArgument(0));

        LibraryBook created = libraryBookService.createLibraryBook(
                MEMBER_1, null, "새 책", "저자", null, null, null, null, null, null, 200
        );

        assertThat(created.getShelfRank()).isNotNull();
        assertThat(last.getShelfRank()).isNotEqualTo(EXHAUSTED_RANK);
        assertThat(handler.names()).containsExactly("library.shelf.rebalance");
        assertThat(handler.highCardinalityValues("library.shelf.book_count")).containsExactly("1");
    }

    @Test
    void 랭크가_넉넉하면_재정렬_span을_남기지_않는다() {
        Shelf shelf = Shelf.create(MEMBER_1, "기본 책장", true);
        when(shelfService.getOrCreateDefaultShelf(MEMBER_1)).thenReturn(shelf);
        when(libraryBookRepository.findLastRanked(shelf.getShelfId())).thenReturn(Optional.empty());
        when(libraryBookRepository.save(any(LibraryBook.class))).thenAnswer(i -> i.getArgument(0));

        libraryBookService.createLibraryBook(
                MEMBER_1, null, "새 책", "저자", null, null, null, null, null, null, 200
        );

        assertThat(handler.names()).isEmpty();
    }
}
