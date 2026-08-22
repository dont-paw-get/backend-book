package com.chc.dpgb.library.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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

import com.chc.dpgb.common.exception.DefaultShelfCannotBeDeletedException;
import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.common.exception.InvalidShelfDataException;
import com.chc.dpgb.common.exception.ShelfAccessDeniedException;
import com.chc.dpgb.common.exception.ShelfNotFoundException;
import com.chc.dpgb.library.application.LibraryBookService;
import com.chc.dpgb.library.application.LibrarySortBy;
import com.chc.dpgb.library.application.ShelfService;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Shelf;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = ShelfController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ShelfController.class})
class ShelfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ShelfService shelfService;

    @MockitoBean
    private LibraryBookService libraryBookService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject("member-1"));
    }

    @Test
    void createShelf는_201과_생성된_책장을_반환한다() throws Exception {
        when(shelfService.createShelf("member-1", "완독한 책"))
                .thenReturn(Shelf.create("member-1", "완독한 책", false));

        mockMvc.perform(post("/api/v1/library/shelves")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"완독한 책"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("완독한 책"))
                .andExpect(jsonPath("$.isDefault").value(false));
    }

    @Test
    void createShelf는_이름이_비어있으면_400() throws Exception {
        when(shelfService.createShelf("member-1", ""))
                .thenThrow(new InvalidShelfDataException("name은 비어 있을 수 없습니다."));

        mockMvc.perform(post("/api/v1/library/shelves")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHELF_DATA"));
    }

    @Test
    void getShelves는_책장_목록과_bookCount를_반환한다() throws Exception {
        Shelf defaultShelf = Shelf.create("member-1", "기본 책장", true);
        when(shelfService.getShelves("member-1")).thenReturn(List.of(defaultShelf));
        when(shelfService.bookCount(defaultShelf.getShelfId())).thenReturn(3L);

        mockMvc.perform(get("/api/v1/library/shelves").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shelves[0].name").value("기본 책장"))
                .andExpect(jsonPath("$.shelves[0].bookCount").value(3));
    }

    @Test
    void updateShelf는_변경된_이름을_반환한다() throws Exception {
        Shelf renamed = Shelf.create("member-1", "다 읽은 책", false);
        when(shelfService.updateShelf("member-1", 2L, "다 읽은 책")).thenReturn(renamed);

        mockMvc.perform(patch("/api/v1/library/shelves/2")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"다 읽은 책"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("다 읽은 책"));
    }

    @Test
    void updateShelf는_이름이_비어있으면_400() throws Exception {
        when(shelfService.updateShelf("member-1", 2L, ""))
                .thenThrow(new InvalidShelfDataException("name은 비어 있을 수 없습니다."));

        mockMvc.perform(patch("/api/v1/library/shelves/2")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHELF_DATA"));
    }

    @Test
    void updateShelf는_권한이_없으면_403() throws Exception {
        when(shelfService.updateShelf("member-1", 2L, "다 읽은 책"))
                .thenThrow(new ShelfAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/shelves/2")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"다 읽은 책"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SHELF_ACCESS_DENIED"));
    }

    @Test
    void updateShelf는_존재하지_않으면_404() throws Exception {
        when(shelfService.updateShelf("member-1", 2L, "다 읽은 책"))
                .thenThrow(new ShelfNotFoundException());

        mockMvc.perform(patch("/api/v1/library/shelves/2")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"다 읽은 책"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHELF_NOT_FOUND"));
    }

    @Test
    void deleteShelf는_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/library/shelves/2").with(member1Jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteShelf는_기본_책장이면_400() throws Exception {
        doThrow(new DefaultShelfCannotBeDeletedException())
                .when(shelfService).deleteShelf("member-1", 2L);

        mockMvc.perform(delete("/api/v1/library/shelves/2").with(member1Jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEFAULT_SHELF_CANNOT_BE_DELETED"));
    }

    @Test
    void deleteShelf는_권한이_없으면_403() throws Exception {
        doThrow(new ShelfAccessDeniedException())
                .when(shelfService).deleteShelf("member-1", 2L);

        mockMvc.perform(delete("/api/v1/library/shelves/2").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SHELF_ACCESS_DENIED"));
    }

    @Test
    void deleteShelf는_존재하지_않으면_404() throws Exception {
        doThrow(new ShelfNotFoundException())
                .when(shelfService).deleteShelf("member-1", 2L);

        mockMvc.perform(delete("/api/v1/library/shelves/2").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHELF_NOT_FOUND"));
    }

    @Test
    void getShelfBooks는_그_책장의_책_목록을_반환한다() throws Exception {
        LibraryBook book = LibraryBook.register(
                "member-1", 2L, "m", "살인자의 기억법", "김영하", null, null, null, null, 100
        );
        Page<LibraryBook> page = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);
        when(libraryBookService.getLibraryBooks(
                eq("member-1"), eq(2L), eq(null), eq(LibrarySortBy.SHELF_ORDER), eq(Sort.Direction.ASC),
                eq(0), eq(20)
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/library/shelves/2/books").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.books[0].title").value("살인자의 기억법"));
    }

    @Test
    void getShelfBooks는_권한이_없으면_403() throws Exception {
        when(shelfService.getOwnedShelf("member-1", 2L)).thenThrow(new ShelfAccessDeniedException());

        mockMvc.perform(get("/api/v1/library/shelves/2/books").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SHELF_ACCESS_DENIED"));
    }

    @Test
    void getShelfBooks는_존재하지_않으면_404() throws Exception {
        when(shelfService.getOwnedShelf("member-1", 2L)).thenThrow(new ShelfNotFoundException());

        mockMvc.perform(get("/api/v1/library/shelves/2/books").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SHELF_NOT_FOUND"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/library/shelves"))
                .andExpect(status().isUnauthorized());
    }
}
