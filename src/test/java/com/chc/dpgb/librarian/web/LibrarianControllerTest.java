package com.chc.dpgb.librarian.web;

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
import com.chc.dpgb.librarian.application.LibrarianService;
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
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/librarians"))
                .andExpect(status().isUnauthorized());
    }
}
