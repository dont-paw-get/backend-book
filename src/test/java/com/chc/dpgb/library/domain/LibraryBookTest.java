package com.chc.dpgb.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LibraryBookTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();

    @Test
    void 등록하면_currentPage는_0으로_시작한다() {
        LibraryBook book = register(160);

        assertThat(book.getCurrentPage()).isZero();
        assertThat(book.getTotalPages()).isEqualTo(160);
        assertThat(book.progress()).isEqualTo(0.0);
    }

    @Test
    void totalPages가_0_이하이면_등록할_수_없다() {
        assertThatThrownBy(() -> register(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> register(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void totalPages를_생략하면_progress는_null이다() {
        LibraryBook book = LibraryBook.register(
                MEMBER_1, 1L, "m", "어린 왕자", "생텍쥐페리", "9788932917245", null,
                "열린책들", LocalDate.of(2015, 10, 20), "https://example.com/cover.jpg", null, null
        );

        assertThat(book.getTotalPages()).isNull();
        assertThat(book.progress()).isNull();
    }

    @Test
    void genre와_readingStatus를_생략하면_기본값이_채워진다() {
        LibraryBook book = LibraryBook.register(
                MEMBER_1, 1L, "m", "어린 왕자", "생텍쥐페리", "9788932917245", null,
                "열린책들", LocalDate.of(2015, 10, 20), "https://example.com/cover.jpg", null, 160
        );

        assertThat(book.getGenre()).isEqualTo(Genre.NONE);
        assertThat(book.getReadingStatus()).isEqualTo(ReadingStatus.PLANNED);
    }

    @Test
    void updateProgress는_currentPage와_progress를_갱신한다() {
        LibraryBook book = register(160);

        book.updateProgress(80, 160);

        assertThat(book.getCurrentPage()).isEqualTo(80);
        assertThat(book.progress()).isEqualTo(50.0);
    }

    @Test
    void currentPage가_totalPages를_초과하면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateProgress(161, 160))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void currentPage가_음수이면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateProgress(-1, 160))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이전_페이지로의_정정은_허용한다() {
        LibraryBook book = register(160);
        book.updateProgress(100, 160);

        book.updateProgress(50, 160);

        assertThat(book.getCurrentPage()).isEqualTo(50);
    }

    @Test
    void 전체_페이지를_기존_현재_페이지보다_작게_줄일_수_없다() {
        LibraryBook book = register(160);
        book.updateProgress(100, 160);

        assertThatThrownBy(() -> book.updateProgress(90, 90))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 현재_페이지에_도달하면_완독_진도율이_된다() {
        LibraryBook book = register(160);

        book.updateProgress(160, 160);

        assertThat(book.progress()).isEqualTo(100.0);
    }

    @Test
    void updateMetadata는_전달한_값으로_모든_필드를_교체한다() {
        LibraryBook book = register(160);

        book.updateMetadata("새 제목", "새 저자", "9791198765432", Genre.FANTASY, "새 출판사",
                LocalDate.of(2020, 1, 1), "https://example.com/new-cover.jpg", ReadingStatus.READING, 200);

        assertThat(book.getTitle()).isEqualTo("새 제목");
        assertThat(book.getAuthor()).isEqualTo("새 저자");
        assertThat(book.getIsbn()).isEqualTo("9791198765432");
        assertThat(book.getGenre()).isEqualTo(Genre.FANTASY);
        assertThat(book.getPublisher()).isEqualTo("새 출판사");
        assertThat(book.getPublishedDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(book.getCoverUrl()).isEqualTo("https://example.com/new-cover.jpg");
        assertThat(book.getReadingStatus()).isEqualTo(ReadingStatus.READING);
        assertThat(book.getTotalPages()).isEqualTo(200);
    }

    @Test
    void updateMetadata에_null을_보내면_선택_필드를_삭제한다() {
        LibraryBook book = register(160);

        book.updateMetadata("어린 왕자", "생텍쥐페리", null, Genre.NONE, null, null, null, ReadingStatus.PLANNED, 160);

        assertThat(book.getIsbn()).isNull();
        assertThat(book.getPublisher()).isNull();
        assertThat(book.getPublishedDate()).isNull();
        assertThat(book.getCoverUrl()).isNull();
        assertThat(book.getTotalPages()).isEqualTo(160);
    }

    @Test
    void updateMetadata의_totalPages는_null로_비울_수_있다() {
        LibraryBook book = register(160);

        book.updateMetadata("어린 왕자", "생텍쥐페리", null, Genre.NONE, null, null, null, ReadingStatus.PLANNED, null);

        assertThat(book.getTotalPages()).isNull();
        assertThat(book.progress()).isNull();
    }

    @Test
    void updateMetadata는_title이_null이면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateMetadata(null, "생텍쥐페리", null, null, null, null, null, null, 160))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMetadata는_author가_null이면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateMetadata("어린 왕자", null, null, null, null, null, null, null, 160))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMetadata의_totalPages도_기존_currentPage보다_작게_줄일_수_없다() {
        LibraryBook book = register(160);
        book.updateProgress(100, 160);

        assertThatThrownBy(() -> book.updateMetadata(
                "어린 왕자", "생텍쥐페리", null, Genre.NONE, null, null, null, ReadingStatus.PLANNED, 90
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMetadata는_genre가_null이면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateMetadata(
                "어린 왕자", "생텍쥐페리", null, null, null, null, null, ReadingStatus.PLANNED, 160
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMetadata는_readingStatus가_null이면_거부한다() {
        LibraryBook book = register(160);

        assertThatThrownBy(() -> book.updateMetadata(
                "어린 왕자", "생텍쥐페리", null, Genre.NONE, null, null, null, null, 160
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeShelfRank는_순서만_변경한다() {
        LibraryBook book = register(160);

        book.changeShelfRank("z");

        assertThat(book.getShelfRank()).isEqualTo("z");
        assertThat(book.getTitle()).isEqualTo("어린 왕자");
    }

    @Test
    void changeShelfId는_책장과_순서를_함께_변경한다() {
        LibraryBook book = register(160);

        book.changeShelfId(2L, "m");

        assertThat(book.getShelfId()).isEqualTo(2L);
        assertThat(book.getShelfRank()).isEqualTo("m");
    }

    @Test
    void softDelete하면_isDeleted가_true가_된다() {
        LibraryBook book = register(160);

        book.softDelete(Instant.now());

        assertThat(book.isDeleted()).isTrue();
    }

    private LibraryBook register(int totalPages) {
        return LibraryBook.register(MEMBER_1, 1L, "m", "어린 왕자", "생텍쥐페리", "9788932917245", null,
                "열린책들", LocalDate.of(2015, 10, 20), "https://example.com/cover.jpg", null, totalPages);
    }
}
