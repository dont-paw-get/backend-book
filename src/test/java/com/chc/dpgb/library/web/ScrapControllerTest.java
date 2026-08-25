package com.chc.dpgb.library.web;

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

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.common.exception.InvalidScrapDataException;
import com.chc.dpgb.common.exception.LibraryBookAccessDeniedException;
import com.chc.dpgb.common.exception.LibraryBookNotFoundException;
import com.chc.dpgb.common.exception.ScrapAccessDeniedException;
import com.chc.dpgb.common.exception.ScrapNotFoundException;
import com.chc.dpgb.library.application.ScrapService;
import com.chc.dpgb.library.domain.Scrap;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = ScrapController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ScrapController.class})
class ScrapControllerTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();
    private static final String IMAGE_URL = "https://example.com/scrap.png";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ScrapService scrapService;

    private static RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject(MEMBER_1.toString()));
    }

    @Test
    void createScrap은_201과_생성된_스크랩을_반환한다() throws Exception {
        when(scrapService.createScrap(MEMBER_1, 1L, "어른들은 누구나 처음엔 어린이였다.", 12, IMAGE_URL, "마음에 남는 문장"))
                .thenReturn(Scrap.create(1L, "어른들은 누구나 처음엔 어린이였다.", 12, IMAGE_URL, "마음에 남는 문장"));

        mockMvc.perform(post("/api/v1/library/books/1/scraps")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"어른들은 누구나 처음엔 어린이였다.","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":"마음에 남는 문장"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.sentence").value("어른들은 누구나 처음엔 어린이였다."))
                .andExpect(jsonPath("$.pageNumber").value(12));
    }

    @Test
    void createScrap은_sentence가_비어있으면_400() throws Exception {
        when(scrapService.createScrap(MEMBER_1, 1L, "", 12, IMAGE_URL, "마음에 남는 문장"))
                .thenThrow(new InvalidScrapDataException("sentence는 비어 있을 수 없습니다."));

        mockMvc.perform(post("/api/v1/library/books/1/scraps")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":"마음에 남는 문장"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCRAP_DATA"));
    }

    @Test
    void createScrap은_책에_대한_권한이_없으면_403() throws Exception {
        when(scrapService.createScrap(MEMBER_1, 1L, "어른들은 누구나 처음엔 어린이였다.", 12, IMAGE_URL, "마음에 남는 문장"))
                .thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(post("/api/v1/library/books/1/scraps")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"어른들은 누구나 처음엔 어린이였다.","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":"마음에 남는 문장"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void createScrap은_책이_존재하지_않으면_404() throws Exception {
        when(scrapService.createScrap(MEMBER_1, 1L, "어른들은 누구나 처음엔 어린이였다.", 12, IMAGE_URL, "마음에 남는 문장"))
                .thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(post("/api/v1/library/books/1/scraps")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"어른들은 누구나 처음엔 어린이였다.","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":"마음에 남는 문장"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void getScraps는_책별_스크랩_목록을_반환한다() throws Exception {
        Scrap scrap = Scrap.create(1L, "문장", 12, IMAGE_URL, null);
        Page<Scrap> page = new PageImpl<>(List.of(scrap), PageRequest.of(0, 20), 1);
        when(scrapService.getScraps(MEMBER_1, 1L, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/library/books/1/scraps").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scraps[0].sentence").value("문장"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getScraps는_책에_대한_권한이_없으면_403() throws Exception {
        when(scrapService.getScraps(MEMBER_1, 1L, 0, 20)).thenThrow(new LibraryBookAccessDeniedException());

        mockMvc.perform(get("/api/v1/library/books/1/scraps").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"));
    }

    @Test
    void getScraps는_책이_존재하지_않으면_404() throws Exception {
        when(scrapService.getScraps(MEMBER_1, 1L, 0, 20)).thenThrow(new LibraryBookNotFoundException());

        mockMvc.perform(get("/api/v1/library/books/1/scraps").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"));
    }

    @Test
    void getScrap은_스크랩_상세를_반환한다() throws Exception {
        when(scrapService.getScrap(MEMBER_1, 456L))
                .thenReturn(Scrap.create(1L, "문장", 12, IMAGE_URL, "메모"));

        mockMvc.perform(get("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentence").value("문장"))
                .andExpect(jsonPath("$.memo").value("메모"));
    }

    @Test
    void getScrap은_권한이_없으면_403() throws Exception {
        when(scrapService.getScrap(MEMBER_1, 456L)).thenThrow(new ScrapAccessDeniedException());

        mockMvc.perform(get("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCRAP_ACCESS_DENIED"));
    }

    @Test
    void getScrap은_존재하지_않으면_404() throws Exception {
        when(scrapService.getScrap(MEMBER_1, 456L)).thenThrow(new ScrapNotFoundException());

        mockMvc.perform(get("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCRAP_NOT_FOUND"));
    }

    @Test
    void updateScrap은_수정된_스크랩을_반환한다() throws Exception {
        when(scrapService.updateScrap(eq(MEMBER_1), eq(456L), eq("새 문장"), eq(12), eq(IMAGE_URL), isNull()))
                .thenReturn(Scrap.create(1L, "새 문장", 12, IMAGE_URL, null));

        mockMvc.perform(patch("/api/v1/library/scraps/456")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"새 문장","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentence").value("새 문장"));
    }

    @Test
    void updateScrap은_sentence가_비어있으면_400() throws Exception {
        when(scrapService.updateScrap(eq(MEMBER_1), eq(456L), eq(""), eq(12), eq(IMAGE_URL), isNull()))
                .thenThrow(new InvalidScrapDataException("sentence는 비어 있을 수 없습니다."));

        mockMvc.perform(patch("/api/v1/library/scraps/456")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCRAP_DATA"));
    }

    @Test
    void updateScrap은_권한이_없으면_403() throws Exception {
        when(scrapService.updateScrap(eq(MEMBER_1), eq(456L), eq("새 문장"), eq(12), eq(IMAGE_URL), isNull()))
                .thenThrow(new ScrapAccessDeniedException());

        mockMvc.perform(patch("/api/v1/library/scraps/456")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"새 문장","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":null}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCRAP_ACCESS_DENIED"));
    }

    @Test
    void updateScrap은_존재하지_않으면_404() throws Exception {
        when(scrapService.updateScrap(eq(MEMBER_1), eq(456L), eq("새 문장"), eq(12), eq(IMAGE_URL), isNull()))
                .thenThrow(new ScrapNotFoundException());

        mockMvc.perform(patch("/api/v1/library/scraps/456")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"새 문장","pageNumber":12,
                                 "scrapImageUrl":"https://example.com/scrap.png","memo":null}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCRAP_NOT_FOUND"));
    }

    @Test
    void deleteScrap은_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteScrap은_권한이_없으면_403() throws Exception {
        doThrow(new ScrapAccessDeniedException()).when(scrapService).deleteScrap(MEMBER_1, 456L);

        mockMvc.perform(delete("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SCRAP_ACCESS_DENIED"));
    }

    @Test
    void deleteScrap은_존재하지_않으면_404() throws Exception {
        doThrow(new ScrapNotFoundException()).when(scrapService).deleteScrap(MEMBER_1, 456L);

        mockMvc.perform(delete("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCRAP_NOT_FOUND"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/library/scraps/456"))
                .andExpect(status().isUnauthorized());
    }
}
