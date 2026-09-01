package com.chc.dpgb.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

/**
 * 운영 환경(prod 프로필)에서 stdout으로 내보내는 구조화 로그 포맷.
 * <p>
 * Spring Boot 내장 {@code ecs}/{@code logstash} 포맷 대신 직접 구현한 이유는 필드명 때문이다 — 내장 포맷은
 * {@code @timestamp}/{@code log.level}/{@code trace.id}처럼 각 규격의 이름을 쓰는데, 이 서비스의 로그 계약은
 * {@code timestamp}/{@code level}/{@code service}/{@code logger}/{@code message}/{@code trace_id}/{@code span_id}로
 * 고정되어 있다(Loki 쿼리가 이 이름에 의존한다). 외부 인코더 라이브러리를 더하지 않고 Boot의
 * {@link JsonWriter}만으로 구현한다.
 * <p>
 * {@code trace_id}/{@code span_id}는 micrometer-tracing이 MDC에 넣는 {@code traceId}/{@code spanId}에서 온다
 * (bridge의 {@code Slf4JEventListener}가 넣는 키 이름). 트레이스 컨텍스트가 없는 로그(기동 로그 등)에서는
 * 두 필드가 빠진다.
 * <p>
 * <b>민감정보 금지</b>: 토큰·비밀번호·{@code Authorization} 헤더·사용자 식별자 MDC 키는 출력하지 않는다.
 * request/response body를 통째로 남기는 필터도 두지 않는다.
 */
public class JsonLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_SPAN_ID = "spanId";

    /**
     * MDC를 top-level로 통과시킬 때 이미 전용 필드로 나간 키는 중복해서 내보내지 않는다.
     */
    private static final Set<String> RESERVED_MDC_KEYS = Set.of(MDC_TRACE_ID, MDC_SPAN_ID, "traceFlags");
    private static final Set<String> BLOCKED_MDC_KEYS = Set.of(
            "authorization",
            "accesstoken",
            "refreshtoken",
            "idtoken",
            "token",
            "password",
            "memberid",
            "cognitosub",
            "sub"
    );

    private final String serviceName;
    private final JsonWriter<ILoggingEvent> jsonWriter;

    public JsonLogFormatter(Environment environment) {
        this.serviceName = resolveServiceName(environment);
        this.jsonWriter = JsonWriter.<ILoggingEvent>of(this::members).withNewLineAtEnd();
    }

    @Override
    public String format(ILoggingEvent event) {
        return this.jsonWriter.writeToString(event);
    }

    private void members(JsonWriter.Members<ILoggingEvent> members) {
        members.add("timestamp", event -> Instant.ofEpochMilli(event.getTimeStamp()).toString());
        members.add("level", event -> event.getLevel().toString());
        members.add("service", this.serviceName);
        members.add("logger", ILoggingEvent::getLoggerName);
        members.add("message", ILoggingEvent::getFormattedMessage);
        members.add("thread", ILoggingEvent::getThreadName);
        members.add("trace_id", event -> mdc(event, MDC_TRACE_ID)).whenNotNull();
        members.add("span_id", event -> mdc(event, MDC_SPAN_ID)).whenNotNull();
        members.add("exception", JsonLogFormatter::throwableClassName).whenNotNull();
        members.add("stack_trace", JsonLogFormatter::stackTrace).whenNotNull();
        members.addMapEntries(JsonLogFormatter::additionalMdc);
    }

    /**
     * OTel resource의 {@code service.name}과 같은 값이 되도록 해석 순서를 맞춘다 —
     * {@code OTEL_SERVICE_NAME}이 있으면 그 값을, 없으면 {@code spring.application.name}을 쓴다.
     */
    private static String resolveServiceName(Environment environment) {
        String otelServiceName = environment.getProperty("OTEL_SERVICE_NAME");
        if (otelServiceName != null && !otelServiceName.isBlank()) {
            return otelServiceName;
        }
        return environment.getProperty("spring.application.name", "unknown");
    }

    private static String mdc(ILoggingEvent event, String key) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        return mdc != null ? mdc.get(key) : null;
    }

    private static Map<String, String> additionalMdc(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null || mdc.isEmpty()) {
            return Map.of();
        }
        Map<String, String> additional = new LinkedHashMap<>();
        mdc.forEach((key, value) -> {
            if (value != null && !RESERVED_MDC_KEYS.contains(key) && !isBlockedMdcKey(key)) {
                additional.put(key, value);
            }
        });
        return additional;
    }

    private static boolean isBlockedMdcKey(String key) {
        return BLOCKED_MDC_KEYS.contains(key.toLowerCase(Locale.ROOT).replace("_", ""));
    }

    private static String throwableClassName(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();
        return throwable != null ? throwable.getClassName() : null;
    }

    private static String stackTrace(ILoggingEvent event) {
        IThrowableProxy throwable = event.getThrowableProxy();
        if (!(throwable instanceof ThrowableProxy proxy)) {
            return null;
        }
        StringWriter writer = new StringWriter();
        proxy.getThrowable().printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
