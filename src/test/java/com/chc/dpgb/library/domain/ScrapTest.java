package com.chc.dpgb.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ScrapTest {

    private static final String IMAGE_URL = "https://example.com/scrap.png";

    @Test
    void 생성하면_전달한_값으로_필드가_채워진다() {
        Scrap scrap = Scrap.create(1L, "어른들은 누구나 처음엔 어린이였다.", 12, IMAGE_URL, "마음에 남는 문장");

        assertThat(scrap.getBookId()).isEqualTo(1L);
        assertThat(scrap.getSentence()).isEqualTo("어른들은 누구나 처음엔 어린이였다.");
        assertThat(scrap.getPageNumber()).isEqualTo(12);
        assertThat(scrap.getScrapImageUrl()).isEqualTo(IMAGE_URL);
        assertThat(scrap.getMemo()).isEqualTo("마음에 남는 문장");
    }

    @Test
    void pageNumber와_memo는_생략할_수_있다() {
        Scrap scrap = Scrap.create(1L, "문장", null, IMAGE_URL, null);

        assertThat(scrap.getPageNumber()).isNull();
        assertThat(scrap.getMemo()).isNull();
    }

    @Test
    void sentence가_null이면_생성할_수_없다() {
        assertThatThrownBy(() -> Scrap.create(1L, null, null, IMAGE_URL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sentence가_빈_문자열이면_생성할_수_없다() {
        assertThatThrownBy(() -> Scrap.create(1L, "   ", null, IMAGE_URL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageNumber가_1보다_작으면_생성할_수_없다() {
        assertThatThrownBy(() -> Scrap.create(1L, "문장", 0, IMAGE_URL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scrapImageUrl이_null이면_생성할_수_없다() {
        assertThatThrownBy(() -> Scrap.create(1L, "문장", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scrapImageUrl이_빈_문자열이면_생성할_수_없다() {
        assertThatThrownBy(() -> Scrap.create(1L, "문장", null, "   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update는_전달한_값으로_모든_필드를_교체한다() {
        Scrap scrap = Scrap.create(1L, "원래 문장", 12, IMAGE_URL, "원래 메모");

        scrap.update("새 문장", 20, "https://example.com/new-scrap.png", "새 메모");

        assertThat(scrap.getSentence()).isEqualTo("새 문장");
        assertThat(scrap.getPageNumber()).isEqualTo(20);
        assertThat(scrap.getScrapImageUrl()).isEqualTo("https://example.com/new-scrap.png");
        assertThat(scrap.getMemo()).isEqualTo("새 메모");
    }

    @Test
    void update에_null을_보내면_pageNumber와_memo를_삭제한다() {
        Scrap scrap = Scrap.create(1L, "원래 문장", 12, IMAGE_URL, "원래 메모");

        scrap.update("원래 문장", null, IMAGE_URL, null);

        assertThat(scrap.getPageNumber()).isNull();
        assertThat(scrap.getMemo()).isNull();
    }

    @Test
    void update는_sentence가_null이면_거부한다() {
        Scrap scrap = Scrap.create(1L, "원래 문장", null, IMAGE_URL, null);

        assertThatThrownBy(() -> scrap.update(null, null, IMAGE_URL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void softDelete하면_isDeleted가_true가_된다() {
        Scrap scrap = Scrap.create(1L, "문장", null, IMAGE_URL, null);

        scrap.softDelete(Instant.now());

        assertThat(scrap.isDeleted()).isTrue();
    }
}
