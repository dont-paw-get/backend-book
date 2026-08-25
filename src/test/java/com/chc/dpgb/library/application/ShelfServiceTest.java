package com.chc.dpgb.library.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.chc.dpgb.common.exception.DefaultShelfCannotBeDeletedException;
import com.chc.dpgb.common.exception.ShelfAccessDeniedException;
import com.chc.dpgb.common.exception.ShelfNotFoundException;
import com.chc.dpgb.library.domain.Shelf;

@ExtendWith(MockitoExtension.class)
class ShelfServiceTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final UUID MEMBER_2 = UUID.randomUUID();

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private LibraryBookRepository libraryBookRepository;

    private ShelfService shelfService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        shelfService = new ShelfService(shelfRepository, libraryBookRepository);
    }

    @Test
    void 기본_책장이_이미_있으면_그대로_반환한다() {
        Shelf existing = Shelf.create(MEMBER_1, "기본 책장", true);
        when(shelfRepository.findDefaultShelf(MEMBER_1)).thenReturn(Optional.of(existing));

        Shelf result = shelfService.getOrCreateDefaultShelf(MEMBER_1);

        assertThat(result).isSameAs(existing);
        verify(shelfRepository, never()).save(any());
    }

    @Test
    void 기본_책장이_없으면_새로_만든다() {
        when(shelfRepository.findDefaultShelf(MEMBER_1)).thenReturn(Optional.empty());
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shelf result = shelfService.getOrCreateDefaultShelf(MEMBER_1);

        assertThat(result.isDefault()).isTrue();
        assertThat(result.getMemberId()).isEqualTo(MEMBER_1);
    }

    @Test
    void 동시_생성으로_유일성_제약을_위반하면_다시_조회한다() {
        Shelf createdByRace = Shelf.create(MEMBER_1, "기본 책장", true);
        when(shelfRepository.findDefaultShelf(MEMBER_1))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(createdByRace));
        when(shelfRepository.save(any(Shelf.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        Shelf result = shelfService.getOrCreateDefaultShelf(MEMBER_1);

        assertThat(result).isSameAs(createdByRace);
    }

    @Test
    void 책장_목록_조회는_기본_책장을_먼저_만들어_둔다() {
        when(shelfRepository.findDefaultShelf(MEMBER_1)).thenReturn(Optional.empty());
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shelfRepository.findAllOwned(MEMBER_1)).thenReturn(List.of());

        shelfService.getShelves(MEMBER_1);

        verify(shelfRepository).save(any(Shelf.class));
    }

    @Test
    void 책장_이름을_바꾼다() {
        Shelf shelf = Shelf.create(MEMBER_1, "옛 이름", false);
        when(shelfRepository.findById(1L)).thenReturn(Optional.of(shelf));
        when(shelfRepository.save(any(Shelf.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shelf result = shelfService.updateShelf(MEMBER_1, 1L, "새 이름");

        assertThat(result.getName()).isEqualTo("새 이름");
    }

    @Test
    void 존재하지_않는_책장을_찾으면_404() {
        when(shelfRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shelfService.getOwnedShelf(MEMBER_1, 1L))
                .isInstanceOf(ShelfNotFoundException.class);
    }

    @Test
    void 다른_사용자의_책장에_접근하면_403() {
        Shelf shelf = Shelf.create(MEMBER_2, "책장", false);
        when(shelfRepository.findById(1L)).thenReturn(Optional.of(shelf));

        assertThatThrownBy(() -> shelfService.getOwnedShelf(MEMBER_1, 1L))
                .isInstanceOf(ShelfAccessDeniedException.class);
    }

    @Test
    void 기본_책장은_삭제할_수_없다() {
        Shelf defaultShelf = Shelf.create(MEMBER_1, "기본 책장", true);
        when(shelfRepository.findById(1L)).thenReturn(Optional.of(defaultShelf));

        assertThatThrownBy(() -> shelfService.deleteShelf(MEMBER_1, 1L))
                .isInstanceOf(DefaultShelfCannotBeDeletedException.class);
    }
}
