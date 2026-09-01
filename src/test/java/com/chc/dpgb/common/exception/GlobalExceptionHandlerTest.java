package com.chc.dpgb.common.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.security.SecurityConfig;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        GlobalExceptionHandlerTest.ThrowingController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void BadRequestException_하위_예외는_400과_stable_code를_반환한다() throws Exception {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            mockMvc.perform(get("/global-exception-handler-test/bad-request").with(jwt()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SEARCH_PARAMETER"))
                    .andExpect(jsonPath("$.message").value("유효한 isbn이 필요합니다."));

            org.assertj.core.api.Assertions.assertThat(logs.list).isEmpty();
        } finally {
            detach(logs);
        }
    }

    @Test
    void ForbiddenException_하위_예외는_403과_stable_code를_반환한다() throws Exception {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            mockMvc.perform(get("/global-exception-handler-test/forbidden").with(jwt()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_ACCESS_DENIED"))
                    .andExpect(jsonPath("$.message").value("해당 도서에 접근할 권한이 없습니다."));

            org.assertj.core.api.Assertions.assertThat(logs.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("권한 거부로 403 응답 code=LIBRARY_BOOK_ACCESS_DENIED");
            org.assertj.core.api.Assertions.assertThat(logs.list)
                    .extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.INFO);
        } finally {
            detach(logs);
        }
    }

    @Test
    void NotFoundException_하위_예외는_404와_stable_code를_반환한다() throws Exception {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            mockMvc.perform(get("/global-exception-handler-test/not-found").with(jwt()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("LIBRARY_BOOK_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("서재에서 해당 도서를 찾을 수 없습니다."));

            org.assertj.core.api.Assertions.assertThat(logs.list).isEmpty();
        } finally {
            detach(logs);
        }
    }

    @Test
    void ConflictException_하위_예외는_409와_stable_code를_반환한다() throws Exception {
        ListAppender<ILoggingEvent> logs = captureLogs();
        try {
            mockMvc.perform(get("/global-exception-handler-test/conflict").with(jwt()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("BOOK_ALREADY_REGISTERED"))
                    .andExpect(jsonPath("$.message").value("이미 서재에 등록된 도서입니다."));

            org.assertj.core.api.Assertions.assertThat(logs.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("비즈니스 충돌로 409 응답 code=BOOK_ALREADY_REGISTERED");
            org.assertj.core.api.Assertions.assertThat(logs.list)
                    .extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.INFO);
        } finally {
            detach(logs);
        }
    }

    @Test
    void BadGatewayException_하위_예외는_502와_stable_code를_반환한다() throws Exception {
        mockMvc.perform(get("/global-exception-handler-test/bad-gateway").with(jwt()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ALADIN_API_ERROR"))
                .andExpect(jsonPath("$.message").value("외부 도서 정보 조회 중 오류가 발생했습니다."));
    }

    @Test
    void 예기치_못한_예외는_500과_INTERNAL_ERROR를_반환한다() throws Exception {
        mockMvc.perform(get("/global-exception-handler-test/unexpected").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."));
    }

    private static ListAppender<ILoggingEvent> captureLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detach(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logger.detachAppender(appender);
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/global-exception-handler-test/bad-request")
        public void badRequest() {
            throw new InvalidSearchParameterException();
        }

        @GetMapping("/global-exception-handler-test/forbidden")
        public void forbidden() {
            throw new LibraryBookAccessDeniedException();
        }

        @GetMapping("/global-exception-handler-test/not-found")
        public void notFound() {
            throw new LibraryBookNotFoundException();
        }

        @GetMapping("/global-exception-handler-test/conflict")
        public void conflict() {
            throw new BookAlreadyRegisteredException();
        }

        @GetMapping("/global-exception-handler-test/bad-gateway")
        public void badGateway() {
            throw new AladinApiException();
        }

        @GetMapping("/global-exception-handler-test/unexpected")
        public void unexpected() {
            throw new IllegalStateException("boom");
        }
    }
}
