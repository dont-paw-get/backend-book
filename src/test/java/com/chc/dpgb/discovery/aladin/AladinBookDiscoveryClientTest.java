package com.chc.dpgb.discovery.aladin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import com.chc.dpgb.common.exception.AladinApiException;
import com.chc.dpgb.discovery.ExternalBook;

class AladinBookDiscoveryClientTest {

    private static final String SEARCH_URL = "https://www.aladin.co.kr/ttb/api/ItemSearch.aspx";

    private MockRestServiceServer mockServer;
    private AladinBookDiscoveryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AladinBookDiscoveryClient(builder, "test-ttb-key");
    }

    @Test
    void title만_주어지면_QueryType_Title로_검색한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andExpect(queryParam("QueryType", "Title"))
                .andExpect(queryParam("Query", encode("어린 왕자")))
                .andRespond(withSuccess("{\"item\":[]}", MediaType.APPLICATION_JSON));

        client.search("어린 왕자", null);

        mockServer.verify();
    }

    @Test
    void author만_주어지면_QueryType_Author로_검색한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andExpect(queryParam("QueryType", "Author"))
                .andExpect(queryParam("Query", encode("김영하")))
                .andRespond(withSuccess("{\"item\":[]}", MediaType.APPLICATION_JSON));

        client.search(null, "김영하");

        mockServer.verify();
    }

    @Test
    void 둘_다_주어지면_QueryType_Keyword로_검색한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andExpect(queryParam("QueryType", "Keyword"))
                .andExpect(queryParam("Query", encode("어린 왕자 생텍쥐페리")))
                .andRespond(withSuccess("{\"item\":[]}", MediaType.APPLICATION_JSON));

        client.search("어린 왕자", "생텍쥐페리");

        mockServer.verify();
    }

    @Test
    void 검색_결과를_ExternalBook으로_변환한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andRespond(withSuccess("""
                        {"item":[{"title":"어린왕자 (소프트커버 에디션) - 개정판",
                        "author":"앙투안 드 생텍쥐페리 (지은이), 전성자 (옮긴이)",
                        "pubDate":"2020-09-22","isbn":"8931021291","isbn13":"9788931021295",
                        "publisher":"문예출판사","cover":"https://image.aladin.co.kr/cover.jpg","subInfo":{}}]}
                        """, MediaType.APPLICATION_JSON));

        List<ExternalBook> result = client.search("어린 왕자", null);

        assertThat(result).hasSize(1);
        ExternalBook book = result.get(0);
        assertThat(book.title()).isEqualTo("어린왕자 (소프트커버 에디션) - 개정판");
        assertThat(book.author()).isEqualTo("앙투안 드 생텍쥐페리, 전성자");
        assertThat(book.isbn()).isEqualTo("9788931021295");
        assertThat(book.publisher()).isEqualTo("문예출판사");
        assertThat(book.publishedDate()).isEqualTo(LocalDate.of(2020, 9, 22));
        assertThat(book.totalPages()).isNull();
        assertThat(book.coverUrl()).isEqualTo("https://image.aladin.co.kr/cover.jpg");
    }

    @Test
    void 결과가_없으면_빈_목록을_반환한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andRespond(withSuccess("{\"item\":[]}", MediaType.APPLICATION_JSON));

        List<ExternalBook> result = client.search("존재하지 않는 책 제목", null);

        assertThat(result).isEmpty();
    }

    @Test
    void 알라딘이_errorCode를_반환하면_AladinApiException으로_변환한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andRespond(withSuccess("{\"errorCode\":4,\"errorMessage\":\"API출력이 금지된 회원입니다.\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("어린 왕자", null))
                .isInstanceOf(AladinApiException.class);
    }

    @Test
    void isbn13이_없으면_isbn10을_대신_사용한다() {
        mockServer.expect(requestTo(startsWith(SEARCH_URL)))
                .andRespond(withSuccess("""
                        {"item":[{"title":"제목","author":"저자","isbn":"8931021291","isbn13":"K102734432"}]}
                        """, MediaType.APPLICATION_JSON));

        List<ExternalBook> result = client.search("제목", null);

        assertThat(result.get(0).isbn()).isEqualTo("8931021291");
    }

    private static String encode(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }
}
