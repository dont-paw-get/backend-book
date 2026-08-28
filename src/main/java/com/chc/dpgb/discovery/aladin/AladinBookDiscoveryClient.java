package com.chc.dpgb.discovery.aladin;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.chc.dpgb.common.exception.AladinApiException;
import com.chc.dpgb.discovery.BookDiscoveryClient;
import com.chc.dpgb.discovery.ExternalBook;

@Component
@Lazy
class AladinBookDiscoveryClient implements BookDiscoveryClient {

    private static final String LOOKUP_URL = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";

    /**
     * ItemSearch와 달리 ItemLookUp은 존재하지 않는 isbn 조회 시 빈 item 배열이 아니라 errorCode 8({"키에 해당하는 상품이 존재하지 않습니다."})을 반환한다(라이브 호출로 확인).
     * 이 코드만 "찾지 못함"으로 취급하고 나머지 errorCode는 그대로 AladinApiException으로 처리한다.
     */
    private static final int ERROR_CODE_ITEM_NOT_FOUND = 8;

    private final RestClient restClient;
    private final String ttbKey;

    AladinBookDiscoveryClient(
            RestClient.Builder restClientBuilder,
            @Value("${book-service.aladin.ttb-key}") String ttbKey
    ) {
        this.restClient = restClientBuilder.build();
        this.ttbKey = ttbKey;
    }

    @Override
    public Optional<ExternalBook> lookup(String isbn) {
        AladinSearchResponse response;
        try {
            response = restClient.get()
                    .uri(buildLookupUri(isbn))
                    .retrieve()
                    .body(AladinSearchResponse.class);
        } catch (RestClientException e) {
            throw new AladinApiException();
        }

        if (response == null) {
            throw new AladinApiException();
        }
        if (response.errorCode() != null) {
            if (response.errorCode() == ERROR_CODE_ITEM_NOT_FOUND) {
                return Optional.empty();
            }
            throw new AladinApiException();
        }

        List<AladinItem> items = response.item() != null ? response.item() : List.of();
        return items.stream().findFirst().map(AladinBookDiscoveryClient::toExternalBook);
    }

    private URI buildLookupUri(String isbn) {
        String itemIdType = isbn.length() == 13 ? "ISBN13" : "ISBN";
        return UriComponentsBuilder.fromUriString(LOOKUP_URL)
                .queryParam("ttbkey", ttbKey)
                .queryParam("ItemId", isbn)
                .queryParam("ItemIdType", itemIdType)
                .queryParam("output", "js")
                .queryParam("Version", "20131101")
                .queryParam("OptResult", "itemPage")
                .build()
                .encode()
                .toUri();
    }

    private static ExternalBook toExternalBook(AladinItem item) {
        return new ExternalBook(
                item.title(),
                AuthorNameNormalizer.normalize(item.author()),
                resolveIsbn(item),
                item.publisher(),
                parsePublishedDate(item.pubDate()),
                resolveTotalPages(item),
                item.cover()
        );
    }

    private static String resolveIsbn(AladinItem item) {
        if (item.isbn13() != null && item.isbn13().matches("\\d{13}")) {
            return item.isbn13();
        }
        if (item.isbn() != null && item.isbn().matches("\\d{10}")) {
            return item.isbn();
        }
        return null;
    }

    private static LocalDate parsePublishedDate(String pubDate) {
        if (pubDate == null || pubDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(pubDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Integer resolveTotalPages(AladinItem item) {
        return item.subInfo() != null ? item.subInfo().itemPage() : null;
    }
}
