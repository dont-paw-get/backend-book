package com.chc.dpgb.library.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.common.exception.BookAlreadyRegisteredException;
import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.common.exception.InvalidBookDataException;
import com.chc.dpgb.common.exception.InvalidReorderTargetException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;
import com.chc.dpgb.library.application.LibraryBookService;
import com.chc.dpgb.library.application.LibrarySortBy;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = LibraryBookController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, LibraryBookController.class})
class LibraryBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private LibraryBookService libraryBookService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject("member-1"));
    }

    private static LibraryBook book(Long shelfId, String shelfRank) {
        LibraryBook book = LibraryBook.register(
                "member-1", shelfId, shelfRank, "어린 왕자", "생텍쥐페리", "9788932917245", "열린책들",
                LocalDate.of(2015, 10, 20), "https://example.com/cover.jpg", 160
        );
        try {
            var field = LibraryBook.class.getDeclaredField("bookId");
            field.setAccessible(true);
            field.set(book, 123L);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return book;
    }

    @Test
    void createLibraryBook은_201과_등록된_책을_반환한다() throws Exception {
        when(libraryBookService.createLibraryBook(
                eq("member-1"), isNull(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(book(1L, "m"));

        mockMvc.perform(post("/api/v1/library/books")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"앙투안 드 생텍쥐페리","totalPages":160}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shelfRank").value("m"))
                .andExpect(jsonPath("$.title").value("어린 왕자"));
    }

    @Test
    void createLibraryBook은_totalPages가_없으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/library/books")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"앙투안 드 생텍쥐페리"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOK_DATA"));
    }

    @Test
    void createLibraryBook은_이미_등록된_ISBN이면_409() throws Exception {
        when(libraryBookService.createLibraryBook(
                eq("member-1"), isNull(), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenThrow(new BookAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/library/books")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"앙투안 드 생텍쥐페리","isbn":"9788932917245","totalPages":160}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_ALREADY_REGISTERED"));
    }

    @Test
    void getLibraryBooks은_기본_정렬로_페이지를_조회한다() throws Exception {
        Page<LibraryBook> page =
                new PageImpl<>(java.util.List.of(book(1L, "m")), PageRequest.of(0, 20), 1);
        when(libraryBookService.getLibraryBooks(
                eq("member-1"), isNull(), isNull(), eq(LibrarySortBy.SHELF_ORDER), eq(Sort.Direction.ASC),
                eq(0), eq(20)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/library/books").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].title").value("어린 왕자"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getLibraryBooks은_잘못된_sortBy면_400() throws Exception {
        mockMvc.perform(get("/api/v1/library/books").param("sortBy", "NOT_A_SORT").with(member1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER_PARAMETER"));
    }

    @Test
    void getLibraryBooks은_size가_범위를_벗어나면_400() throws Exception {
        mockMvc.perform(get("/api/v1/library/books").param("size", "101").with(member1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER_PARAMETER"));
    }

    @Test
    void getLibraryBook은_상세_정보를_반환한다() throws Exception {
        when(libraryBookService.getLibraryBook("member-1", 123L)).thenReturn(book(1L, "m"));

        mockMvc.perform(get("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9788932917245"));
    }

    @Test
    void getLibraryBook은_권한이_없으면_403() throws Exception {
        when(libraryBookService.getLibraryBook("member-1", 123L))
                .thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(get("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void getLibraryBook은_존재하지_않으면_404() throws Exception {
        when(libraryBookService.getLibraryBook("member-1", 123L))
                .thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(get("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void updateLibraryBook은_수정된_책을_반환한다() throws Exception {
        when(libraryBookService.updateLibraryBook(
                eq("member-1"), eq(123L), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(book(1L, "m"));

        mockMvc.perform(patch("/api/v1/library/books/123")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"생텍쥐페리","isbn":null,"publisher":null,
                                 "publishedDate":null,"coverUrl":null,"totalPages":158}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").isNumber());
    }

    @Test
    void updateLibraryBook은_title이_비어있으면_400() throws Exception {
        when(libraryBookService.updateLibraryBook(
                eq("member-1"), eq(123L), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenThrow(new InvalidBookDataException("title은 비어 있을 수 없습니다."));

        mockMvc.perform(patch("/api/v1/library/books/123")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","author":"생텍쥐페리","isbn":null,"publisher":null,
                                 "publishedDate":null,"coverUrl":null,"totalPages":158}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOK_DATA"));
    }

    @Test
    void updateLibraryBook은_권한이_없으면_403() throws Exception {
        when(libraryBookService.updateLibraryBook(
                eq("member-1"), eq(123L), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/books/123")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"생텍쥐페리","isbn":null,"publisher":null,
                                 "publishedDate":null,"coverUrl":null,"totalPages":158}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void updateLibraryBook은_존재하지_않으면_404() throws Exception {
        when(libraryBookService.updateLibraryBook(
                eq("member-1"), eq(123L), any(), any(), any(), any(), any(), any(), anyInt()
        )).thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(patch("/api/v1/library/books/123")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"어린 왕자","author":"생텍쥐페리","isbn":null,"publisher":null,
                                 "publishedDate":null,"coverUrl":null,"totalPages":158}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void deleteLibraryBook은_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLibraryBook은_권한이_없으면_403() throws Exception {
        doThrow(new LibraryBookAccessDeniedException())
                .when(libraryBookService).deleteLibraryBook("member-1", 123L);

        mockMvc.perform(delete("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void deleteLibraryBook은_존재하지_않으면_404() throws Exception {
        doThrow(new LibraryBookNotFoundException())
                .when(libraryBookService).deleteLibraryBook("member-1", 123L);

        mockMvc.perform(delete("/api/v1/library/books/123").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void reorderLibraryBook은_변경된_shelfRank를_반환한다() throws Exception {
        LibraryBook reordered = book(1L, "mV");
        when(libraryBookService.reorderLibraryBook("member-1", 123L, null, 456L)).thenReturn(reordered);

        mockMvc.perform(patch("/api/v1/library/books/123/order")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"afterBookId":456}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelfRank").value("mV"));
    }

    @Test
    void reorderLibraryBook은_이웃이_다른_책장이면_400() throws Exception {
        when(libraryBookService.reorderLibraryBook("member-1", 123L, null, 456L))
                .thenThrow(new InvalidReorderTargetException());

        mockMvc.perform(patch("/api/v1/library/books/123/order")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"afterBookId":456}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REORDER_TARGET"));
    }

    @Test
    void reorderLibraryBook은_권한이_없으면_403() throws Exception {
        when(libraryBookService.reorderLibraryBook("member-1", 123L, null, 456L))
                .thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/books/123/order")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"afterBookId":456}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void reorderLibraryBook은_존재하지_않으면_404() throws Exception {
        when(libraryBookService.reorderLibraryBook("member-1", 123L, null, 456L))
                .thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(patch("/api/v1/library/books/123/order")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"afterBookId":456}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void moveLibraryBookToShelf는_shelfId가_없으면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/library/books/123/shelf")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHELF_TARGET"));
    }

    @Test
    void moveLibraryBookToShelf는_이동된_책장을_반환한다() throws Exception {
        when(libraryBookService.moveLibraryBookToShelf("member-1", 123L, 2L)).thenReturn(book(2L, "m"));

        mockMvc.perform(patch("/api/v1/library/books/123/shelf")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shelfId":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelfId").value(2));
    }

    @Test
    void moveLibraryBookToShelf는_권한이_없으면_403() throws Exception {
        when(libraryBookService.moveLibraryBookToShelf("member-1", 123L, 2L))
                .thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/books/123/shelf")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shelfId":2}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void moveLibraryBookToShelf는_존재하지_않으면_404() throws Exception {
        when(libraryBookService.moveLibraryBookToShelf("member-1", 123L, 2L))
                .thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(patch("/api/v1/library/books/123/shelf")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shelfId":2}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void updateReadingProgress는_필드가_없으면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/library/books/123/progress")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_VALUE"));
    }

    @Test
    void updateReadingProgress는_갱신된_진도율을_반환한다() throws Exception {
        LibraryBook progressed = book(1L, "m");
        progressed.updateProgress(80, 160);
        when(libraryBookService.updateReadingProgress("member-1", 123L, 80, 160)).thenReturn(progressed);

        mockMvc.perform(patch("/api/v1/library/books/123/progress")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPage":80,"totalPages":160}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(50.0));
    }

    @Test
    void updateReadingProgress는_권한이_없으면_403() throws Exception {
        when(libraryBookService.updateReadingProgress("member-1", 123L, 80, 160))
                .thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/books/123/progress")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPage":80,"totalPages":160}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void updateReadingProgress는_존재하지_않으면_404() throws Exception {
        when(libraryBookService.updateReadingProgress("member-1", 123L, 80, 160))
                .thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(patch("/api/v1/library/books/123/progress")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPage":80,"totalPages":160}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/library/books/123"))
                .andExpect(status().isUnauthorized());
    }
}
