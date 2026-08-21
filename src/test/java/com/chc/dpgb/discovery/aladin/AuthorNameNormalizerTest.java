package com.chc.dpgb.discovery.aladin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthorNameNormalizerTest {

    @Test
    void 단일_저자의_역할_라벨을_제거한다() {
        assertThat(AuthorNameNormalizer.normalize("김영하 (지은이)")).isEqualTo("김영하");
    }

    @Test
    void 복수_저자를_쉼표로_구분해_반환한다() {
        assertThat(AuthorNameNormalizer.normalize("앙투안 드 생텍쥐페리 (지은이), 전성자 (옮긴이)"))
                .isEqualTo("앙투안 드 생텍쥐페리, 전성자");
    }

    @Test
    void 역할_라벨이_없어도_그대로_반환한다() {
        assertThat(AuthorNameNormalizer.normalize("김영하")).isEqualTo("김영하");
    }

    @Test
    void null이면_null을_반환한다() {
        assertThat(AuthorNameNormalizer.normalize(null)).isNull();
    }
}
