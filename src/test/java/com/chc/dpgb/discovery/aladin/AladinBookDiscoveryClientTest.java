package com.chc.dpgb.discovery.aladin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.chc.dpgb.common.exception.AladinApiException;
import com.chc.dpgb.discovery.ExternalBook;

class AladinBookDiscoveryClientTest {

    private static final String LOOKUP_URL = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";
    private static final String NOT_FOUND_RESPONSE = "{\"errorCode\":8,\"errorMessage\":\"키에 해당하는 상품이 존재하지 않습니다.\"}";

    private MockRestServiceServer mockServer;
    private AladinBookDiscoveryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new AladinBookDiscoveryClient(builder, "test-ttb-key");
    }

    @Test
    void isbn13이면_ItemIdType_ISBN13으로_조회한다() {
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andExpect(queryParam("ItemIdType", "ISBN13"))
                .andExpect(queryParam("ItemId", "9788931021295"))
                .andRespond(withSuccess(NOT_FOUND_RESPONSE, MediaType.APPLICATION_JSON));

        client.lookup("9788931021295");

        mockServer.verify();
    }

    @Test
    void isbn10이면_ItemIdType_ISBN으로_조회한다() {
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andExpect(queryParam("ItemIdType", "ISBN"))
                .andExpect(queryParam("ItemId", "8931021291"))
                .andRespond(withSuccess(NOT_FOUND_RESPONSE, MediaType.APPLICATION_JSON));

        client.lookup("8931021291");

        mockServer.verify();
    }

    @Test
    void 조회_결과를_ExternalBook으로_변환한다() {
        // 실제 ItemLookUp(ItemId=9788932917245, ItemIdType=ISBN13) 라이브 호출로 캡처한 응답
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andRespond(withSuccess("""
                        {"version":"20131101","title":"알라딘 상품정보 - 어린 왕자","totalResults":1,
                        "item":[{"title":"어린 왕자",
                        "author":"앙투안 드 생텍쥐페리 (지은이), 황현산 (옮긴이)",
                        "pubDate":"2015-10-20","isbn":"8932917248","isbn13":"9788932917245",
                        "publisher":"열린책들","cover":"https://image.aladin.co.kr/product/6853/49/coversum/8932917248_2.jpg",
                        "subInfo":{"itemPage":136}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<ExternalBook> result = client.lookup("9788932917245");

        assertThat(result).isPresent();
        ExternalBook book = result.get();
        assertThat(book.title()).isEqualTo("어린 왕자");
        assertThat(book.author()).isEqualTo("앙투안 드 생텍쥐페리, 황현산");
        assertThat(book.isbn()).isEqualTo("9788932917245");
        assertThat(book.publisher()).isEqualTo("열린책들");
        assertThat(book.publishedDate()).isEqualTo(LocalDate.of(2015, 10, 20));
        assertThat(book.totalPages()).isEqualTo(136);
        assertThat(book.coverUrl()).isEqualTo("https://image.aladin.co.kr/product/6853/49/coversum/8932917248_2.jpg");
    }

    @Test
    void 존재하지_않는_isbn이면_빈_Optional을_반환한다() {
        // ItemSearch(결과 없음 = 빈 item 배열)와 달리 ItemLookUp은 없는 isbn 조회 시 errorCode 8을 반환한다(라이브 호출로 확인)
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andRespond(withSuccess(NOT_FOUND_RESPONSE, MediaType.APPLICATION_JSON));

        Optional<ExternalBook> result = client.lookup("9788900000000");

        assertThat(result).isEmpty();
    }

    @Test
    void 알라딘이_상품없음_이외의_errorCode를_반환하면_AladinApiException으로_변환한다() {
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andRespond(withSuccess("{\"errorCode\":4,\"errorMessage\":\"API출력이 금지된 회원입니다.\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.lookup("9788931021295"))
                .isInstanceOf(AladinApiException.class);
    }

    @Test
    void isbn13이_없으면_isbn10을_대신_사용한다() {
        mockServer.expect(requestTo(startsWith(LOOKUP_URL)))
                .andRespond(withSuccess("""
                        {"item":[{"title":"제목","author":"저자","isbn":"8931021291","isbn13":"K102734432"}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<ExternalBook> result = client.lookup("8931021291");

        assertThat(result).isPresent();
        assertThat(result.get().isbn()).isEqualTo("8931021291");
    }
}
