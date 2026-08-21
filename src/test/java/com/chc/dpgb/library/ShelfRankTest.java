package com.chc.dpgb.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ShelfRankTest {

    private static final String VALID_PATTERN = "^[0-9A-Za-z]+$";

    @Test
    void initial_값은_패턴을_만족한다() {
        String initial = ShelfRank.initial();

        assertThat(initial).matches(VALID_PATTERN);
    }

    @Test
    void after는_주어진_값보다_사전식으로_뒤에_온다() {
        String first = ShelfRank.initial();

        String second = ShelfRank.after(first);

        assertThat(second).matches(VALID_PATTERN);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void before는_주어진_값보다_사전식으로_앞에_온다() {
        String first = ShelfRank.initial();

        String zero = ShelfRank.before(first);

        assertThat(zero).matches(VALID_PATTERN);
        assertThat(zero).isLessThan(first);
    }

    @Test
    void between은_두_값_사이의_값을_반환한다() {
        String lower = "A";
        String upper = "z";

        String mid = ShelfRank.between(lower, upper);

        assertThat(mid).matches(VALID_PATTERN);
        assertThat(mid).isGreaterThan(lower);
        assertThat(mid).isLessThan(upper);
    }

    @Test
    void 인접한_값_사이에서도_유효한_값을_찾는다() {
        String lower = "A";
        String upper = "B";

        String mid = ShelfRank.between(lower, upper);

        assertThat(mid).matches(VALID_PATTERN);
        assertThat(mid).isGreaterThan(lower);
        assertThat(mid).isLessThan(upper);
    }

    @Test
    void 반복_삽입해도_매번_사이_값을_찾는다() {
        String lower = "A";
        String upper = "B";

        for (int i = 0; i < 20; i++) {
            String mid = ShelfRank.between(lower, upper);
            assertThat(mid).isGreaterThan(lower).isLessThan(upper);
            upper = mid;
        }
    }

    @Test
    void prev가_next보다_사전식으로_앞서지_않으면_예외() {
        assertThatThrownBy(() -> ShelfRank.between("B", "A"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ShelfRank.between("A", "A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 두_값_사이_공간이_소진되면_예외를_던진다() {
        String prev = "A".repeat(200);
        String next = prev + "B";

        assertThatThrownBy(() -> ShelfRank.between(prev, next))
                .isInstanceOf(ShelfRankExhaustedException.class);
    }

    @Test
    void rebalancedSequence는_count가_0이면_빈_배열을_반환한다() {
        assertThat(ShelfRank.rebalancedSequence(0)).isEmpty();
    }

    @Test
    void rebalancedSequence는_오름차순_고유값을_생성한다() {
        String[] keys = ShelfRank.rebalancedSequence(50);

        assertThat(keys).hasSize(50);
        assertThat(keys).allMatch(key -> key.matches(VALID_PATTERN));
        for (int i = 1; i < keys.length; i++) {
            assertThat(keys[i]).isGreaterThan(keys[i - 1]);
        }
    }
}
