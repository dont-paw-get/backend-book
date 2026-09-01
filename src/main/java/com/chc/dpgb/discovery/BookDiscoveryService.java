package com.chc.dpgb.discovery;

import java.util.UUID;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.chc.dpgb.common.exception.InvalidSearchParameterException;
import com.chc.dpgb.library.application.LibraryBookRepository;

@Service
public class BookDiscoveryService {

    private static final String ISBN_PATTERN = "^(?:[0-9]{10}|[0-9]{13})$";

    /**
     * 자동 계측만으로는 "이 요청이 왜 알라딘을 호출했는지"를 알 수 없다 — 서재에 이미 있으면 외부 호출 없이 끝나기 때문에,
     * trace에는 외부 호출 span이 있는 요청과 없는 요청이 이유 없이 섞여 보인다. 이 span의 outcome 속성이 그 분기를 설명한다.
     */
    private static final String SEARCH_OBSERVATION_NAME = "book.discovery.search";
    private static final String OUTCOME_KEY = "book.discovery.outcome";

    private final LibraryBookRepository libraryBookRepository;
    private final BookDiscoveryClient bookDiscoveryClient;
    private final ObservationRegistry observationRegistry;

    /**
     * bookDiscoveryClient는 실제로는 ALADIN_API_TTB_KEY를 읽는 @Lazy 빈이다. 주입 지점에도 @Lazy를 붙여야 지연 프록시가 생성되고, 그렇지 않으면 이 생성자가 즉시
     * 실행될 때 실제 빈이 만들어지며 자격 증명 placeholder가 즉시 해석된다.
     */
    BookDiscoveryService(
            LibraryBookRepository libraryBookRepository,
            @Lazy BookDiscoveryClient bookDiscoveryClient,
            ObservationRegistry observationRegistry
    ) {
        this.libraryBookRepository = libraryBookRepository;
        this.bookDiscoveryClient = bookDiscoveryClient;
        this.observationRegistry = observationRegistry;
    }

    public BookSearchResult search(UUID memberId, String isbn) {
        if (isbn == null || !isbn.matches(ISBN_PATTERN)) {
            throw new InvalidSearchParameterException();
        }
        Observation observation = Observation.start(SEARCH_OBSERVATION_NAME, observationRegistry);
        try (Observation.Scope ignored = observation.openScope()) {
            BookSearchResult result = doSearch(memberId, isbn);
            observation.lowCardinalityKeyValue(OUTCOME_KEY, outcomeOf(result));
            return result;
        } catch (Exception e) {
            observation.error(e);
            throw e;
        } finally {
            observation.stop();
        }
    }

    private BookSearchResult doSearch(UUID memberId, String isbn) {
        return libraryBookRepository.findByMemberIdAndIsbn(memberId, isbn)
                .<BookSearchResult>map(BookSearchResult::alreadyRegistered)
                .orElseGet(() -> bookDiscoveryClient.lookup(isbn)
                        .map(BookSearchResult::found)
                        .orElseGet(BookSearchResult::notFound));
    }

    private static String outcomeOf(BookSearchResult result) {
        if (result.alreadyRegistered()) {
            return "ALREADY_REGISTERED";
        }
        return result.book() != null ? "FOUND" : "NOT_FOUND";
    }
}
