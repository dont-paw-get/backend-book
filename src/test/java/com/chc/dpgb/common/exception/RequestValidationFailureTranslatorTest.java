package com.chc.dpgb.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import com.chc.dpgb.librarian.web.dto.AcquireLibrarianRequest;
import com.chc.dpgb.librarian.web.dto.RenameLibrarianRequest;
import com.chc.dpgb.library.web.dto.CreateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.CreateScrapRequest;
import com.chc.dpgb.library.web.dto.CreateShelfRequest;
import com.chc.dpgb.library.web.dto.MoveLibraryBookToShelfRequest;
import com.chc.dpgb.library.web.dto.ReorderLibraryBookRequest;
import com.chc.dpgb.library.web.dto.UpdateLibraryBookRequest;
import com.chc.dpgb.library.web.dto.UpdateReadingProgressRequest;
import com.chc.dpgb.library.web.dto.UpdateScrapRequest;
import com.chc.dpgb.library.web.dto.UpdateShelfRequest;

/**
 * 검증 실패가 어떤 계약 코드로 나가는지를 고정한다. 기대값은 {@code docs/api/openapi.yaml}의 각 operation이 선언한 400
 * 응답에서 그대로 가져왔다 (ADR-0013).
 */
class RequestValidationFailureTranslatorTest {

    static Stream<Arguments> 계약이_정한_요청별_400_코드() {
        return Stream.of(
                Arguments.of(CreateLibraryBookRequest.class, "INVALID_BOOK_DATA"),
                Arguments.of(UpdateLibraryBookRequest.class, "INVALID_BOOK_DATA"),
                Arguments.of(ReorderLibraryBookRequest.class, "INVALID_REORDER_TARGET"),
                Arguments.of(MoveLibraryBookToShelfRequest.class, "INVALID_SHELF_TARGET"),
                Arguments.of(UpdateReadingProgressRequest.class, "INVALID_PAGE_VALUE"),
                Arguments.of(CreateShelfRequest.class, "INVALID_SHELF_DATA"),
                Arguments.of(UpdateShelfRequest.class, "INVALID_SHELF_DATA"),
                Arguments.of(CreateScrapRequest.class, "INVALID_SCRAP_DATA"),
                Arguments.of(UpdateScrapRequest.class, "INVALID_SCRAP_DATA"),
                Arguments.of(AcquireLibrarianRequest.class, "INVALID_LIBRARIAN_DATA"),
                Arguments.of(RenameLibrarianRequest.class, "INVALID_LIBRARIAN_DATA")
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("계약이_정한_요청별_400_코드")
    void 요청_타입을_계약이_정한_400_코드로_옮긴다(Class<?> requestType, String expectedCode) {
        BadRequestException translated = RequestValidationFailureTranslator.translate(requestType);

        assertThat(translated).isNotNull();
        assertThat(translated.code()).isEqualTo(expectedCode);
        assertThat(translated.getMessage()).isNotBlank();
    }

    @Test
    void 매핑에_없는_타입이면_null을_돌려준다() {
        assertThat(RequestValidationFailureTranslator.translate(String.class)).isNull();
        assertThat(RequestValidationFailureTranslator.supports(String.class)).isFalse();
    }

    /**
     * 새 요청 DTO를 추가하면서 매핑을 빠뜨리면 검증 실패가 500으로 나간다. 컴파일러가 잡아주지 않는 자리라 여기서 잡는다.
     */
    @Test
    void 모든_요청_DTO에_매핑이_있다() {
        List<Class<?>> requestDtos = scanRequestDtos();

        // 스캐너가 아무것도 못 찾으면 아래 검증이 공허하게 통과한다 — 먼저 스캔 자체가 동작했음을 확인한다
        assertThat(requestDtos).hasSizeGreaterThanOrEqualTo(11);

        List<Class<?>> missing = requestDtos.stream()
                .filter(type -> !RequestValidationFailureTranslator.supports(type))
                .toList();

        assertThat(missing)
                .describedAs("RequestValidationFailureTranslator에 매핑을 추가해야 하는 요청 DTO")
                .isEmpty();
    }

    private List<Class<?>> scanRequestDtos() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*[.]web[.]dto[.].*Request$")));
        List<Class<?>> found = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.chc.dpgb")) {
            try {
                found.add(Class.forName(definition.getBeanClassName()));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return found;
    }
}
