package com.chc.dpgb.discovery.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.discovery.BookDiscoveryService;
import com.chc.dpgb.discovery.BookSearchResult;
import com.chc.dpgb.discovery.ExternalBook;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = BookDiscoveryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, BookDiscoveryController.class})
class BookDiscoveryControllerTest {

    private static final String MEMBER_1 = UUID.randomUUID().toString();
    private static final String ISBN = "9788932917245";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private BookDiscoveryService bookDiscoveryService;

    @Test
    void 알라딘에서_찾으면_book을_반환한다() throws Exception {
        when(bookDiscoveryService.search(UUID.fromString(MEMBER_1), ISBN))
                .thenReturn(BookSearchResult.found(new ExternalBook(
                        "어린 왕자", "생텍쥐페리", ISBN, "열린책들", null, 160, "https://example.com/cover.jpg"
                )));

        mockMvc.perform(get("/api/v1/books/search").param("isbn", ISBN).with(jwt().jwt(j -> j.subject(MEMBER_1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyRegistered").value(false))
                .andExpect(jsonPath("$.book.title").value("어린 왕자"))
                .andExpect(jsonPath("$.book.isbn").value(ISBN))
                .andExpect(jsonPath("$.libraryBook").doesNotExist());
    }

    @Test
    void 이미_서재에_등록되어_있으면_libraryBook을_반환한다() throws Exception {
        LibraryBook book = LibraryBook.register(
                UUID.fromString(MEMBER_1), 1L, "m", "어린 왕자", "생텍쥐페리", ISBN, null, null, null, null, null, 160
        );
        when(bookDiscoveryService.search(UUID.fromString(MEMBER_1), ISBN))
                .thenReturn(BookSearchResult.alreadyRegistered(book));

        mockMvc.perform(get("/api/v1/books/search").param("isbn", ISBN).with(jwt().jwt(j -> j.subject(MEMBER_1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyRegistered").value(true))
                .andExpect(jsonPath("$.libraryBook.title").value("어린 왕자"))
                .andExpect(jsonPath("$.libraryBook.isbn").value(ISBN))
                .andExpect(jsonPath("$.book").doesNotExist());
    }

    @Test
    void 알라딘에도_없으면_alreadyRegistered만_false로_반환한다() throws Exception {
        when(bookDiscoveryService.search(UUID.fromString(MEMBER_1), ISBN))
                .thenReturn(BookSearchResult.notFound());

        mockMvc.perform(get("/api/v1/books/search").param("isbn", ISBN).with(jwt().jwt(j -> j.subject(MEMBER_1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyRegistered").value(false))
                .andExpect(jsonPath("$.book").doesNotExist())
                .andExpect(jsonPath("$.libraryBook").doesNotExist());
    }

    @Test
    void isbn이_없으면_400() throws Exception {
        when(bookDiscoveryService.search(UUID.fromString(MEMBER_1), null))
                .thenThrow(new com.chc.dpgb.common.exception.InvalidSearchParameterException());

        mockMvc.perform(get("/api/v1/books/search").with(jwt().jwt(j -> j.subject(MEMBER_1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_PARAMETER"));
    }

    @Test
    void 알라딘_API가_오류를_반환하면_502() throws Exception {
        when(bookDiscoveryService.search(UUID.fromString(MEMBER_1), ISBN))
                .thenThrow(new com.chc.dpgb.common.exception.AladinApiException());

        mockMvc.perform(get("/api/v1/books/search").param("isbn", ISBN).with(jwt().jwt(j -> j.subject(MEMBER_1))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ALADIN_API_ERROR"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/books/search").param("isbn", ISBN))
                .andExpect(status().isUnauthorized());
    }
}
