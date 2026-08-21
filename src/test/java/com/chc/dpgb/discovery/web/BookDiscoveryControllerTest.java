package com.chc.dpgb.discovery.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.discovery.BookDiscoveryService;
import com.chc.dpgb.discovery.ExternalBook;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = BookDiscoveryController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, BookDiscoveryController.class})
class BookDiscoveryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private BookDiscoveryService bookDiscoveryService;

	@Test
	void searchBookInfo는_검색_결과를_반환한다() throws Exception {
		when(bookDiscoveryService.search("어린 왕자", null))
				.thenReturn(List.of(new ExternalBook(
						"어린 왕자", "생텍쥐페리", "9788932917245", "열린책들", null, 160,
						"https://example.com/cover.jpg")));

		mockMvc.perform(get("/api/v1/books/search").param("title", "어린 왕자").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.books[0].title").value("어린 왕자"))
				.andExpect(jsonPath("$.books[0].isbn").value("9788932917245"));
	}

	@Test
	void searchBookInfo는_결과가_없으면_빈_배열을_반환한다() throws Exception {
		when(bookDiscoveryService.search("존재하지 않음", null)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/books/search").param("title", "존재하지 않음").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.books").isEmpty());
	}

	@Test
	void title과_author가_둘_다_없으면_400() throws Exception {
		when(bookDiscoveryService.search(null, null))
				.thenThrow(new com.chc.dpgb.common.exception.InvalidSearchParameterException());

		mockMvc.perform(get("/api/v1/books/search").with(jwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_SEARCH_PARAMETER"));
	}
}
