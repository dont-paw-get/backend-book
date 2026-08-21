package com.chc.dpgb.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.chc.dpgb.common.exception.InvalidSearchParameterException;

@ExtendWith(MockitoExtension.class)
class BookDiscoveryServiceTest {

    @Mock
    private BookDiscoveryClient bookDiscoveryClient;

    private BookDiscoveryService bookDiscoveryService;

    @BeforeEach
    void setUp() {
        bookDiscoveryService = new BookDiscoveryService(bookDiscoveryClient);
    }

    @Test
    void title과_author가_둘_다_없으면_400() {
        assertThatThrownBy(() -> bookDiscoveryService.search(null, null))
                .isInstanceOf(InvalidSearchParameterException.class);
        assertThatThrownBy(() -> bookDiscoveryService.search("  ", ""))
                .isInstanceOf(InvalidSearchParameterException.class);
    }

    @Test
    void title만_있어도_검색한다() {
        when(bookDiscoveryClient.search("어린 왕자", null))
                .thenReturn(List.of(new ExternalBook("어린 왕자", "생텍쥐페리", null, null, null, null, null)));

        List<ExternalBook> result = bookDiscoveryService.search("어린 왕자", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void author만_있어도_검색한다() {
        when(bookDiscoveryClient.search(null, "김영하")).thenReturn(List.of());

        List<ExternalBook> result = bookDiscoveryService.search(null, "김영하");

        assertThat(result).isEmpty();
    }
}
