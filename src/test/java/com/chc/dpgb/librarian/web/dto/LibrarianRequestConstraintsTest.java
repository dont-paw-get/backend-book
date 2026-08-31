package com.chc.dpgb.librarian.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.chc.dpgb.librarian.domain.LibrarianType;

/**
 * 사서 요청 DTO의 제약이 {@code docs/api/openapi.yaml}의 선언과 일치하는지 경계값으로 고정한다 (ADR-0013).
 */
class LibrarianRequestConstraintsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> violatedFields(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static String repeat(int length) {
        return "가".repeat(length);
    }

    @Test
    void 사서_획득_요청은_type과_name이_모두_필수다() {
        assertThat(violatedFields(new AcquireLibrarianRequest(LibrarianType.RUSSIAN_BLUE, "나비"))).isEmpty();
        assertThat(violatedFields(new AcquireLibrarianRequest(null, "나비"))).contains("type");
        assertThat(violatedFields(new AcquireLibrarianRequest(LibrarianType.SHOEBILL, null))).contains("name");
        assertThat(violatedFields(new AcquireLibrarianRequest(LibrarianType.SHOEBILL, ""))).contains("name");
    }

    @Test
    void 사서_이름은_50자까지_허용하고_51자부터_위반이다() {
        assertThat(violatedFields(new AcquireLibrarianRequest(LibrarianType.SHOEBILL, repeat(50)))).isEmpty();
        assertThat(violatedFields(new AcquireLibrarianRequest(LibrarianType.SHOEBILL, repeat(51))))
                .contains("name");
        assertThat(violatedFields(new RenameLibrarianRequest(repeat(50)))).isEmpty();
        assertThat(violatedFields(new RenameLibrarianRequest(repeat(51)))).contains("name");
    }

    @Test
    void 사서_이름_변경_요청은_null이나_빈_이름을_거부한다() {
        assertThat(violatedFields(new RenameLibrarianRequest(null))).contains("name");
        assertThat(violatedFields(new RenameLibrarianRequest(""))).contains("name");
    }
}
