package com.chc.dpgb.discovery.aladin;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

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

	private static final String SEARCH_URL = "https://www.aladin.co.kr/ttb/api/ItemSearch.aspx";
	private static final int MAX_RESULTS = 10;

	private final RestClient restClient;
	private final String ttbKey;

	AladinBookDiscoveryClient(
			RestClient.Builder restClientBuilder,
			@Value("${book-service.aladin.ttb-key}") String ttbKey) {
		this.restClient = restClientBuilder.build();
		this.ttbKey = ttbKey;
	}

	@Override
	public List<ExternalBook> search(String title, String author) {
		AladinSearchResponse response;
		try {
			response = restClient.get()
					.uri(buildSearchUri(title, author))
					.retrieve()
					.body(AladinSearchResponse.class);
		} catch (RestClientException e) {
			throw new AladinApiException();
		}

		if (response == null || response.errorCode() != null) {
			throw new AladinApiException();
		}

		List<AladinItem> items = response.item() != null ? response.item() : List.of();
		return items.stream().map(AladinBookDiscoveryClient::toExternalBook).toList();
	}

	private URI buildSearchUri(String title, String author) {
		boolean hasTitle = title != null && !title.isBlank();
		boolean hasAuthor = author != null && !author.isBlank();

		String query;
		String queryType;
		if (hasTitle && hasAuthor) {
			query = title + " " + author;
			queryType = "Keyword";
		} else if (hasTitle) {
			query = title;
			queryType = "Title";
		} else {
			query = author;
			queryType = "Author";
		}

		return UriComponentsBuilder.fromUriString(SEARCH_URL)
				.queryParam("ttbkey", ttbKey)
				.queryParam("Query", query)
				.queryParam("QueryType", queryType)
				.queryParam("SearchTarget", "Book")
				.queryParam("MaxResults", MAX_RESULTS)
				.queryParam("start", 1)
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
				item.cover());
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
