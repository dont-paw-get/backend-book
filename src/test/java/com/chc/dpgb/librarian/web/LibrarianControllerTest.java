package com.chc.dpgb.librarian.web;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.chc.dpgb.common.exception.GlobalExceptionHandler;
import com.chc.dpgb.common.exception.LibrarianAccessDeniedException;
import com.chc.dpgb.common.exception.LibrarianAlreadyOwnedException;
import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.RepresentativeLibrarianNotSelectedException;
import com.chc.dpgb.librarian.application.LibrarianService;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;
import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = LibrarianController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, LibrarianController.class})
class LibrarianControllerTest {

    private static final UUID MEMBER_1 = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private LibrarianService librarianService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor member1Jwt() {
        return jwt().jwt(builder -> builder.subject(MEMBER_1.toString()));
    }

    private static Librarian librarian(Long librarianId, LibrarianType type, String name) {
        Librarian librarian = Librarian.acquire(MEMBER_1, type, name);
        try {
            var field = Librarian.class.getDeclaredField("librarianId");
            field.setAccessible(true);
            field.set(librarian, librarianId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return librarian;
    }

    @Test
    void getLibrarianTypes는_타입_카탈로그를_반환한다() throws Exception {
        LibrarianTypeInfo typeInfo = new LibrarianTypeInfo(
                LibrarianType.RUSSIAN_BLUE, "https://example.com/cat.png", "https://example.com/cat-clicked.png"
        );
        when(librarianService.getLibrarianTypes()).thenReturn(List.of(typeInfo));

        mockMvc.perform(get("/api/v1/librarian-types").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.types[0].type").value("RUSSIAN_BLUE"))
                .andExpect(jsonPath("$.types[0].imageUrl").value("https://example.com/cat.png"));
    }

    @Test
    void acquireLibrarian은_201과_획득한_사서를_반환한다() throws Exception {
        when(librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"))
                .thenReturn(librarian(1L, LibrarianType.RUSSIAN_BLUE, "나비"));

        mockMvc.perform(post("/api/v1/librarians")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"RUSSIAN_BLUE","name":"나비"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("나비"))
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void acquireLibrarian은_같은_타입을_이미_보유하면_409() throws Exception {
        when(librarianService.acquireLibrarian(MEMBER_1, LibrarianType.RUSSIAN_BLUE, "나비"))
                .thenThrow(new LibrarianAlreadyOwnedException());

        mockMvc.perform(post("/api/v1/librarians")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"RUSSIAN_BLUE","name":"나비"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_ALREADY_OWNED"));
    }

    @Test
    void getLibrarians는_보유한_사서_목록을_반환한다() throws Exception {
        when(librarianService.getLibrarians(MEMBER_1))
                .thenReturn(List.of(librarian(1L, LibrarianType.RUSSIAN_BLUE, "나비")));

        mockMvc.perform(get("/api/v1/librarians").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.librarians[0].name").value("나비"))
                .andExpect(jsonPath("$.librarians[0].type").value("RUSSIAN_BLUE"));
    }

    @Test
    void renameLibrarian은_변경된_이름을_반환한다() throws Exception {
        when(librarianService.renameLibrarian(MEMBER_1, 1L, "루루"))
                .thenReturn(librarian(1L, LibrarianType.RUSSIAN_BLUE, "루루"));

        mockMvc.perform(patch("/api/v1/librarians/1")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"루루"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("루루"));
    }

    @Test
    void renameLibrarian은_존재하지_않으면_404() throws Exception {
        when(librarianService.renameLibrarian(MEMBER_1, 1L, "루루"))
                .thenThrow(new LibrarianNotFoundException());

        mockMvc.perform(patch("/api/v1/librarians/1")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"루루"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_NOT_FOUND"));
    }

    @Test
    void renameLibrarian은_권한이_없으면_403() throws Exception {
        when(librarianService.renameLibrarian(MEMBER_1, 1L, "루루"))
                .thenThrow(new LibrarianAccessDeniedException());

        mockMvc.perform(patch("/api/v1/librarians/1")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"루루"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_ACCESS_DENIED"));
    }

    @Test
    void selectRepresentative는_대표_사서를_반환한다() throws Exception {
        Librarian representative = librarian(1L, LibrarianType.RUSSIAN_BLUE, "나비");
        representative.markAsRepresentative();
        when(librarianService.selectRepresentative(MEMBER_1, 1L)).thenReturn(representative);

        mockMvc.perform(patch("/api/v1/librarians/1/representative").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.librarianId").value(1));
    }

    @Test
    void getRepresentative는_대표_사서를_반환한다() throws Exception {
        Librarian representative = librarian(1L, LibrarianType.RUSSIAN_BLUE, "나비");
        representative.markAsRepresentative();
        when(librarianService.getRepresentative(MEMBER_1)).thenReturn(representative);

        mockMvc.perform(get("/api/v1/librarians/representative").with(member1Jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("나비"));
    }

    @Test
    void getRepresentative는_선택된_적_없으면_404() throws Exception {
        when(librarianService.getRepresentative(MEMBER_1))
                .thenThrow(new RepresentativeLibrarianNotSelectedException());

        mockMvc.perform(get("/api/v1/librarians/representative").with(member1Jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPRESENTATIVE_LIBRARIAN_NOT_SELECTED"));
    }

    @Test
    void deleteLibrarian은_204를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/librarians/1").with(member1Jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLibrarian은_권한이_없으면_403() throws Exception {
        doThrow(new LibrarianAccessDeniedException()).when(librarianService).deleteLibrarian(MEMBER_1, 1L);

        mockMvc.perform(delete("/api/v1/librarians/1").with(member1Jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LIBRARIAN_ACCESS_DENIED"));
    }

    @Test
    void 인증되지_않으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/librarians"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 사서_획득_요청에_type이_없으면_400_INVALID_LIBRARIAN_DATA() throws Exception {
        mockMvc.perform(post("/api/v1/librarians")
                        .with(member1Jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"나비\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LIBRARIAN_DATA"));
    }
}
