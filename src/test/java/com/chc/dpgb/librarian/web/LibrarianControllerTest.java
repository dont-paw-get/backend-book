package com.chc.dpgb.librarian.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.LibrarianNotSelectedException;
import com.chc.dpgb.librarian.application.LibrarianService;
import com.chc.dpgb.librarian.application.SelectedLibrarian;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = LibrarianController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, LibrarianController.class})
class LibrarianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private LibrarianService librarianService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject("member-1"));
    }

    @Test
    void getLibrarians는_마스터_목록을_반환한다() throws Exception {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        when(librarianService.getLibrarians()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/librarians").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.librarians[0].name").value("러시안블루"))
                .andExpect(jsonPath("$.librarians[0].type").value("CAT"));
    }

    @Test
    void getMyLibrarian은_선택한_대표_사서를_반환한다() throws Exception {
        Librarian cat = new Librarian(1L, "러시안블루", "CAT", "https://example.com/librarians/cat-1.png", 1);
        Instant selectedAt = Instant.parse("2026-08-20T00:00:00Z");
        when(librarianService.getMyLibrarian("member-1"))
                .thenReturn(new SelectedLibrarian(cat, selectedAt));

        mockMvc.perform(get("/api/v1/members/me/librarian").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.librarianId").value(1))
                .andExpect(jsonPath("$.name").value("러시안블루"));
    }

    @Test
    void getMyLibrarian은_미선택이면_404를_반환한다() throws Exception {
        when(librarianService.getMyLibrarian("member-1")).thenThrow(new LibrarianNotSelectedException());

        mockMvc.perform(get("/api/v1/members/me/librarian").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_NOT_SELECTED"));
    }

    @Test
    void selectMyLibrarian은_선택_결과를_반환한다() throws Exception {
        Librarian bird = new Librarian(2L, "슈빌", "BIRD", "https://example.com/librarians/bird-1.png", 1);
        Instant selectedAt = Instant.parse("2026-08-21T00:00:00Z");
        when(librarianService.selectMyLibrarian("member-1", 2L))
                .thenReturn(new SelectedLibrarian(bird, selectedAt));

        mockMvc.perform(put("/api/v1/members/me/librarian")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"librarianId":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.librarianId").value(2))
                .andExpect(jsonPath("$.name").value("슈빌"));
    }

    @Test
    void selectMyLibrarian은_존재하지_않는_사서면_404를_반환한다() throws Exception {
        when(librarianService.selectMyLibrarian("member-1", 99L))
                .thenThrow(new LibrarianNotFoundException());

        mockMvc.perform(put("/api/v1/members/me/librarian")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"librarianId":99}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_NOT_FOUND"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/librarians"))
                .andExpect(status().isUnauthorized());
    }
}
