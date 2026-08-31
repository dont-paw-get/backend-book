package com.chc.dpgb.library.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.chc.dpgb.library.domain.Genre;
import com.chc.dpgb.library.domain.ReadingStatus;

/**
 * 요청 DTO의 제약이 {@code docs/api/openapi.yaml}의 선언과 일치하는지 경계값으로 고정한다 (ADR-0013).
 */
class LibraryRequestConstraintsTest {

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

    private static CreateLibraryBookRequest create(String title, String author, String isbn,
                                                   String publisher, Integer totalPages, Long shelfId) {
        return new CreateLibraryBookRequest(title, author, isbn, null, publisher, null, totalPages, null,
                null, shelfId);
    }

    // ---------- CreateLibraryBookRequest ----------

    @Test
    void 필수값만_채운_등록_요청은_위반이_없다() {
        assertThat(violatedFields(create("어린 왕자", "생텍쥐페리", null, null, null, null))).isEmpty();
    }

    @Test
    void 등록_요청의_title은_null이거나_비어_있으면_위반이다() {
        assertThat(violatedFields(create(null, "저자", null, null, null, null))).contains("title");
        assertThat(violatedFields(create("", "저자", null, null, null, null))).contains("title");
    }

    @Test
    void 등록_요청의_title은_200자까지_허용하고_201자부터_위반이다() {
        assertThat(violatedFields(create(repeat(200), "저자", null, null, null, null))).isEmpty();
        assertThat(violatedFields(create(repeat(201), "저자", null, null, null, null))).contains("title");
    }

    @Test
    void 등록_요청의_author는_100자까지_허용하고_101자부터_위반이다() {
        assertThat(violatedFields(create("제목", repeat(100), null, null, null, null))).isEmpty();
        assertThat(violatedFields(create("제목", repeat(101), null, null, null, null))).contains("author");
    }

    @Test
    void 등록_요청의_isbn은_10자리_또는_13자리_숫자만_허용한다() {
        assertThat(violatedFields(create("제목", "저자", "1234567890", null, null, null))).isEmpty();
        assertThat(violatedFields(create("제목", "저자", "9788932917245", null, null, null))).isEmpty();
        assertThat(violatedFields(create("제목", "저자", null, null, null, null))).isEmpty();
        assertThat(violatedFields(create("제목", "저자", "123456789012", null, null, null))).contains("isbn");
        assertThat(violatedFields(create("제목", "저자", "abcdefghij", null, null, null))).contains("isbn");
    }

    @Test
    void 등록_요청의_publisher는_100자까지_허용한다() {
        assertThat(violatedFields(create("제목", "저자", null, repeat(100), null, null))).isEmpty();
        assertThat(violatedFields(create("제목", "저자", null, repeat(101), null, null))).contains("publisher");
    }

    @Test
    void 등록_요청의_totalPages와_shelfId는_1_이상이어야_한다() {
        assertThat(violatedFields(create("제목", "저자", null, null, 1, 1L))).isEmpty();
        assertThat(violatedFields(create("제목", "저자", null, null, 0, null))).contains("totalPages");
        assertThat(violatedFields(create("제목", "저자", null, null, null, 0L))).contains("shelfId");
    }

    // ---------- UpdateLibraryBookRequest ----------

    @Test
    void 수정_요청은_삭제_의미의_null을_허용하고_필수_4개만_강제한다() {
        UpdateLibraryBookRequest clearingOptionalFields = new UpdateLibraryBookRequest(
                "제목", "저자", null, Genre.NONE, null, null, null, ReadingStatus.PLANNED, null);

        assertThat(violatedFields(clearingOptionalFields)).isEmpty();
    }

    @Test
    void 수정_요청의_genre와_readingStatus는_null일_수_없다() {
        UpdateLibraryBookRequest request = new UpdateLibraryBookRequest(
                "제목", "저자", null, null, null, LocalDate.of(2020, 1, 1), null, null, null);

        assertThat(violatedFields(request)).containsExactlyInAnyOrder("genre", "readingStatus");
    }

    // ---------- ReorderLibraryBookRequest ----------

    @Test
    void 재정렬_요청은_before나_after_중_정확히_하나만_지정해야_한다() {
        assertThat(violatedFields(new ReorderLibraryBookRequest(1L, null))).isEmpty();
        assertThat(violatedFields(new ReorderLibraryBookRequest(null, 2L))).isEmpty();
        assertThat(violatedFields(new ReorderLibraryBookRequest(null, null)))
                .contains("exactlyOneTargetSpecified");
        assertThat(violatedFields(new ReorderLibraryBookRequest(1L, 2L)))
                .contains("exactlyOneTargetSpecified");
    }

    // ---------- MoveLibraryBookToShelfRequest ----------

    @Test
    void 책장_이동_요청의_shelfId는_필수이며_1_이상이다() {
        assertThat(violatedFields(new MoveLibraryBookToShelfRequest(1L))).isEmpty();
        assertThat(violatedFields(new MoveLibraryBookToShelfRequest(null))).contains("shelfId");
        assertThat(violatedFields(new MoveLibraryBookToShelfRequest(0L))).contains("shelfId");
    }

    // ---------- UpdateReadingProgressRequest ----------

    @Test
    void 진도_수정_요청의_currentPage는_필수이며_0_이상이다() {
        assertThat(violatedFields(new UpdateReadingProgressRequest(0, null))).isEmpty();
        assertThat(violatedFields(new UpdateReadingProgressRequest(null, null))).contains("currentPage");
        assertThat(violatedFields(new UpdateReadingProgressRequest(-1, null))).contains("currentPage");
    }

    @Test
    void 진도_수정_요청의_totalPages는_null이면_삭제이고_0은_위반이다() {
        assertThat(violatedFields(new UpdateReadingProgressRequest(10, null))).isEmpty();
        assertThat(violatedFields(new UpdateReadingProgressRequest(10, 0))).contains("totalPages");
    }

    // ---------- Scrap ----------

    @Test
    void 스크랩_등록_요청은_sentence와_scrapImageUrl이_필수다() {
        assertThat(violatedFields(new CreateScrapRequest("문장", 1, "https://img", null))).isEmpty();
        assertThat(violatedFields(new CreateScrapRequest(null, null, "https://img", null)))
                .contains("sentence");
        assertThat(violatedFields(new CreateScrapRequest("", null, "https://img", null))).contains("sentence");
        assertThat(violatedFields(new CreateScrapRequest("문장", null, null, null))).contains("scrapImageUrl");
    }

    @Test
    void 스크랩의_sentence와_memo는_2000자까지_허용한다() {
        assertThat(violatedFields(new CreateScrapRequest(repeat(2000), null, "https://img", repeat(2000))))
                .isEmpty();
        assertThat(violatedFields(new CreateScrapRequest(repeat(2001), null, "https://img", null)))
                .contains("sentence");
        assertThat(violatedFields(new CreateScrapRequest("문장", null, "https://img", repeat(2001))))
                .contains("memo");
    }

    @Test
    void 스크랩_수정_요청은_pageNumber와_memo의_null을_삭제로_허용한다() {
        assertThat(violatedFields(new UpdateScrapRequest("문장", null, "https://img", null))).isEmpty();
        assertThat(violatedFields(new UpdateScrapRequest("문장", 0, "https://img", null)))
                .contains("pageNumber");
    }

    // ---------- Shelf ----------

    @Test
    void 책장_이름은_1자_이상_50자_이하다() {
        assertThat(violatedFields(new CreateShelfRequest(repeat(50)))).isEmpty();
        assertThat(violatedFields(new CreateShelfRequest(repeat(51)))).contains("name");
        assertThat(violatedFields(new CreateShelfRequest(""))).contains("name");
        assertThat(violatedFields(new CreateShelfRequest(null))).contains("name");
        assertThat(violatedFields(new UpdateShelfRequest(repeat(51)))).contains("name");
        assertThat(violatedFields(new UpdateShelfRequest("읽는 중"))).isEmpty();
    }
}
