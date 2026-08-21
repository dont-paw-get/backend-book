package com.chc.dpgb.library.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.library.Scrap;
import com.chc.dpgb.library.ScrapService;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = ScrapController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ScrapController.class})
class ScrapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ScrapService scrapService;

    private static RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject("member-1"));
    }

    @Test
    void createScrap은_201과_생성된_스크랩을_반환한다() throws Exception {
        when(scrapService.createScrap("member-1", 1L, "어른들은 누구나 처음엔 어린이였다.", 12, "마음에 남는 문장"))
                .thenReturn(Scrap.create(1L, "어른들은 누구나 처음엔 어린이였다.", 12, "마음에 남는 문장"));

        mockMvc.perform(post("/api/v1/library/books/1/scraps")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"어른들은 누구나 처음엔 어린이였다.","pageNumber":12,"memo":"마음에 남는 문장"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.sentence").value("어른들은 누구나 처음엔 어린이였다."))
                .andExpect(jsonPath("$.pageNumber").value(12));
    }

    @Test
    void getScraps는_책별_스크랩_목록을_반환한다() throws Exception {
        Scrap scrap = Scrap.create(1L, "문장", 12, null);
        Page<Scrap> page = new PageImpl<>(List.of(scrap), PageRequest.of(0, 20), 1);
        when(scrapService.getScraps("member-1", 1L, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/library/books/1/scraps").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scraps[0].sentence").value("문장"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getScrap은_스크랩_상세를_반환한다() throws Exception {
        when(scrapService.getScrap("member-1", 456L))
                .thenReturn(Scrap.create(1L, "문장", 12, "메모"));

        mockMvc.perform(get("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentence").value("문장"))
                .andExpect(jsonPath("$.memo").value("메모"));
    }

    @Test
    void updateScrap은_수정된_스크랩을_반환한다() throws Exception {
        when(scrapService.updateScrap(eq("member-1"), eq(456L), eq("새 문장"), eq(12), isNull()))
                .thenReturn(Scrap.create(1L, "새 문장", 12, null));

        mockMvc.perform(patch("/api/v1/library/scraps/456")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sentence":"새 문장","pageNumber":12,"memo":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentence").value("새 문장"));
    }

    @Test
    void deleteScrap은_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/library/scraps/456").with(member1Jwt()))
                .andExpect(status().isNoContent());
    }
}
