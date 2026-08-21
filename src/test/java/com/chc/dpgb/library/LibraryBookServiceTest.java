package com.chc.dpgb.library;

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

import com.chc.dpgb.common.exception.BookAlreadyRegisteredException;
import com.chc.dpgb.common.exception.InvalidBookDataException;
import com.chc.dpgb.common.exception.InvalidPageValueException;
import com.chc.dpgb.common.exception.InvalidReorderTargetException;
import com.chc.dpgb.common.exception.InvalidShelfTargetException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;

@ExtendWith(MockitoExtension.class)
class LibraryBookServiceTest {

    @Mock
    private LibraryBookRepository libraryBookRepository;

    @Mock
    private ShelfRepository shelfRepository;

    @Mock
    private ShelfService shelfService;

    private LibraryBookService libraryBookService;

    @BeforeEach
    void setUp() {
        libraryBookService = new LibraryBookService(libraryBookRepository, shelfRepository, shelfService);
    }

    private void stubSaveToReturnItsArgument() {
        when(libraryBookRepository.save(any(LibraryBook.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static LibraryBook book(Long bookId, String memberId, Long shelfId, String shelfRank) {
        LibraryBook book = LibraryBook.register(
                memberId, shelfId, shelfRank, "제목", "저자", null, null, null, null, 100
        );
        setBookId(book, bookId);
        return book;
    }

    private static void setBookId(LibraryBook book, Long bookId) {
        try {
            var field = LibraryBook.class.getDeclaredField("bookId");
            field.setAccessible(true);
            field.set(book, bookId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void shelfId를_생략하면_기본_책장에_등록한다() {
        stubSaveToReturnItsArgument();
        Shelf defaultShelf = Shelf.create("member-1", "기본 책장", true);
        setShelfId(defaultShelf, 1L);
        when(shelfService.getOrCreateDefaultShelf("member-1")).thenReturn(defaultShelf);
        when(libraryBookRepository.findLastRanked(1L)).thenReturn(Optional.empty());

        LibraryBook result = libraryBookService.createLibraryBook(
                "member-1", null, "제목", "저자", null, null, null, null, 100
        );

        assertThat(result.getShelfId()).isEqualTo(1L);
        assertThat(result.getShelfRank()).isEqualTo(ShelfRank.initial());
    }

    @Test
    void 존재하지_않는_shelfId를_지정하면_400() {
        when(shelfRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryBookService.createLibraryBook(
                "member-1", 99L, "제목", "저자", null, null, null, null, 100
        )).isInstanceOf(InvalidBookDataException.class);
    }

    @Test
    void 남의_shelfId를_지정하면_400() {
        Shelf othersShelf = Shelf.create("member-2", "책장", false);
        setShelfId(othersShelf, 5L);
        when(shelfRepository.findById(5L)).thenReturn(Optional.of(othersShelf));

        assertThatThrownBy(() -> libraryBookService.createLibraryBook(
                "member-1", 5L, "제목", "저자", null, null, null, null, 100
        )).isInstanceOf(InvalidBookDataException.class);
    }

    @Test
    void 이미_등록된_isbn이면_409() {
        Shelf shelf = Shelf.create("member-1", "책장", false);
        setShelfId(shelf, 1L);
        when(shelfRepository.findById(1L)).thenReturn(Optional.of(shelf));
        when(libraryBookRepository.existsByIsbn("member-1", "9788932917245")).thenReturn(true);

        assertThatThrownBy(() -> libraryBookService.createLibraryBook(
                "member-1", 1L, "제목", "저자", "9788932917245", null, null, null, 100
        )).isInstanceOf(BookAlreadyRegisteredException.class);
    }

    @Test
    void 동시_등록으로_유일성_제약을_위반하면_409로_변환한다() {
        Shelf shelf = Shelf.create("member-1", "책장", false);
        setShelfId(shelf, 1L);
        when(shelfRepository.findById(1L)).thenReturn(Optional.of(shelf));
        when(libraryBookRepository.findLastRanked(1L)).thenReturn(Optional.empty());
        when(libraryBookRepository.save(any(LibraryBook.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> libraryBookService.createLibraryBook(
                "member-1", 1L, "제목", "저자", null, null, null, null, 100
        )).isInstanceOf(BookAlreadyRegisteredException.class);
    }

    @Test
    void 존재하지_않는_책을_조회하면_404() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryBookService.getLibraryBook("member-1", 1L))
                .isInstanceOf(LibraryBookNotFoundException.class);
    }

    @Test
    void 남의_책을_조회하면_403() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "member-2", 1L, "m")));

        assertThatThrownBy(() -> libraryBookService.getLibraryBook("member-1", 1L))
                .isInstanceOf(LibraryBookAccessDeniedException.class);
    }

    @Test
    void 잘못된_페이지_값은_400으로_변환한다() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "member-1", 1L, "m")));

        assertThatThrownBy(() -> libraryBookService.updateReadingProgress("member-1", 1L, 200, 100))
                .isInstanceOf(InvalidPageValueException.class);
    }

    @Test
    void reorder는_beforeBookId와_afterBookId_둘_다_없으면_400() {
        assertThatThrownBy(() -> libraryBookService.reorderLibraryBook("member-1", 1L, null, null))
                .isInstanceOf(InvalidReorderTargetException.class);
    }

    @Test
    void reorder는_beforeBookId와_afterBookId_둘_다_있으면_400() {
        assertThatThrownBy(() -> libraryBookService.reorderLibraryBook("member-1", 1L, 2L, 3L))
                .isInstanceOf(InvalidReorderTargetException.class);
    }

    @Test
    void reorder는_자기_자신을_기준으로_지정할_수_없다() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "member-1", 1L, "m")));

        assertThatThrownBy(() -> libraryBookService.reorderLibraryBook("member-1", 1L, null, 1L))
                .isInstanceOf(InvalidReorderTargetException.class);
    }

    @Test
    void reorder는_다른_책장의_책을_기준으로_지정할_수_없다() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "member-1", 1L, "m")));
        when(libraryBookRepository.findById(2L)).thenReturn(Optional.of(book(2L, "member-1", 2L, "m")));

        assertThatThrownBy(() -> libraryBookService.reorderLibraryBook("member-1", 1L, null, 2L))
                .isInstanceOf(InvalidReorderTargetException.class);
    }

    @Test
    void reorder는_두_책_사이의_shelfRank를_계산해_저장한다() {
        stubSaveToReturnItsArgument();
        LibraryBook moving = book(1L, "member-1", 1L, "a");
        LibraryBook first = book(2L, "member-1", 1L, "m");
        LibraryBook second = book(3L, "member-1", 1L, "z");
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(moving));
        when(libraryBookRepository.findById(3L)).thenReturn(Optional.of(second));
        when(libraryBookRepository.findShelfOrderedByRank(1L))
                .thenReturn(new java.util.ArrayList<>(List.of(moving, first, second)));

        LibraryBook result = libraryBookService.reorderLibraryBook("member-1", 1L, 3L, null);

        assertThat(result.getShelfRank()).isGreaterThan("m");
        assertThat(result.getShelfRank()).isLessThan("z");
    }

    @Test
    void moveLibraryBookToShelf는_존재하지_않는_책장이면_400() {
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(book(1L, "member-1", 1L, "m")));
        when(shelfRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> libraryBookService.moveLibraryBookToShelf("member-1", 1L, 9L))
                .isInstanceOf(InvalidShelfTargetException.class);
    }

    @Test
    void moveLibraryBookToShelf는_대상_책장_맨_뒤로_배치한다() {
        stubSaveToReturnItsArgument();
        LibraryBook moving = book(1L, "member-1", 1L, "m");
        Shelf target = Shelf.create("member-1", "다른 책장", false);
        setShelfId(target, 2L);
        when(libraryBookRepository.findById(1L)).thenReturn(Optional.of(moving));
        when(shelfRepository.findById(2L)).thenReturn(Optional.of(target));
        when(libraryBookRepository.findLastRanked(2L))
                .thenReturn(Optional.of(book(4L, "member-1", 2L, "m")));

        LibraryBook result = libraryBookService.moveLibraryBookToShelf("member-1", 1L, 2L);

        assertThat(result.getShelfId()).isEqualTo(2L);
        assertThat(result.getShelfRank()).isGreaterThan("m");
    }

    private static void setShelfId(Shelf shelf, Long shelfId) {
        try {
            var field = Shelf.class.getDeclaredField("shelfId");
            field.setAccessible(true);
            field.set(shelf, shelfId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
