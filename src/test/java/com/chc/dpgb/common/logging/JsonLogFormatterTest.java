package com.chc.dpgb.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.mock.env.MockEnvironment;

/**
 * 이 포맷터의 계약은 "Loki가 조회할 필드 이름과 값"이다. 내부 구현(JsonWriter 호출 순서 등)이 아니라 실제로 나온 JSON 한 줄을
 * 파싱해 결과를 검증한다.
 */
class JsonLogFormatterTest {

    private static final String LOGGER_NAME = "com.chc.dpgb.library.application.LibraryBookService";

    @Test
    void 필수_필드를_약속된_이름으로_내보낸다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("OTEL_SERVICE_NAME", "backend-book"));

        Map<String, Object> json = format(formatter, event(Level.INFO, "서재 책 등록 bookId=7", Map.of(
                "traceId", "0af7651916cd43dd8448eb211c80319c",
                "spanId", "b7ad6b7169203331"
        )));

        assertThat(json).containsOnlyKeys(
                "timestamp", "level", "service", "logger", "message", "thread", "trace_id", "span_id"
        );
        assertThat(json.get("level")).isEqualTo("INFO");
        assertThat(json.get("service")).isEqualTo("backend-book");
        assertThat(json.get("logger")).isEqualTo(LOGGER_NAME);
        assertThat(json.get("message")).isEqualTo("서재 책 등록 bookId=7");
        assertThat(json.get("trace_id")).isEqualTo("0af7651916cd43dd8448eb211c80319c");
        assertThat(json.get("span_id")).isEqualTo("b7ad6b7169203331");
        assertThat((String) json.get("timestamp")).endsWith("Z");
    }

    @Test
    void 트레이스_컨텍스트가_없으면_trace_id와_span_id를_넣지_않는다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("OTEL_SERVICE_NAME", "backend-book"));

        Map<String, Object> json = format(formatter, event(Level.INFO, "기동 로그", Map.of()));

        assertThat(json).doesNotContainKeys("trace_id", "span_id");
    }

    @Test
    void 예외가_있으면_예외_타입과_스택트레이스를_함께_남긴다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("OTEL_SERVICE_NAME", "backend-book"));
        LoggingEvent event = event(Level.ERROR, "처리되지 않은 예외로 500 응답", Map.of());
        event.setThrowableProxy(new ch.qos.logback.classic.spi.ThrowableProxy(new IllegalStateException("boom")));

        Map<String, Object> json = format(formatter, event);

        assertThat(json.get("exception")).isEqualTo("java.lang.IllegalStateException");
        assertThat((String) json.get("stack_trace")).contains("java.lang.IllegalStateException: boom");
    }

    @Test
    void OTEL_SERVICE_NAME이_없으면_spring_application_name을_쓴다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("spring.application.name", "dpgb"));

        Map<String, Object> json = format(formatter, event(Level.INFO, "메시지", Map.of()));

        assertThat(json.get("service")).isEqualTo("dpgb");
    }

    @Test
    void 트레이스_외의_MDC는_top_level로_통과시킨다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("OTEL_SERVICE_NAME", "backend-book"));

        Map<String, Object> json = format(formatter, event(Level.WARN, "메시지", Map.of(
                "traceId", "0af7651916cd43dd8448eb211c80319c",
                "spanId", "b7ad6b7169203331",
                "traceFlags", "01",
                "requestId", "req-1"
        )));

        assertThat(json.get("requestId")).isEqualTo("req-1");
        assertThat(json).doesNotContainKeys("traceId", "spanId", "traceFlags");
    }

    @Test
    void 사용자_식별자와_credential_MDC는_출력하지_않는다() {
        JsonLogFormatter formatter = formatterWith(new MockEnvironment()
                .withProperty("OTEL_SERVICE_NAME", "backend-book"));

        Map<String, Object> json = format(formatter, event(Level.INFO, "메시지", Map.of(
                "memberId", "123e4567-e89b-12d3-a456-426614174000",
                "cognito_sub", "123e4567-e89b-12d3-a456-426614174000",
                "sub", "123e4567-e89b-12d3-a456-426614174000",
                "Authorization", "Bearer secret",
                "accessToken", "secret",
                "password", "secret",
                "requestId", "req-1"
        )));

        assertThat(json).doesNotContainKeys(
                "memberId", "cognito_sub", "sub", "Authorization", "accessToken", "password"
        );
        assertThat(json.get("requestId")).isEqualTo("req-1");
    }

    private JsonLogFormatter formatterWith(MockEnvironment environment) {
        return new JsonLogFormatter(environment);
    }

    private Map<String, Object> format(JsonLogFormatter formatter, ILoggingEvent event) {
        return JsonParserFactory.getJsonParser().parseMap(formatter.format(event));
    }

    private LoggingEvent event(Level level, String message, Map<String, String> mdc) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerName(LOGGER_NAME);
        event.setLevel(level);
        event.setMessage(message);
        event.setThreadName("http-nio-8080-exec-1");
        event.setTimeStamp(System.currentTimeMillis());
        event.setMDCPropertyMap(mdc);
        event.setLoggerContext(new LoggerContext());
        return event;
    }
}
