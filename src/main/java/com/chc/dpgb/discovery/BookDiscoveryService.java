package com.chc.dpgb.discovery;

import java.util.UUID;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.chc.dpgb.common.exception.InvalidSearchParameterException;
import com.chc.dpgb.library.application.LibraryBookRepository;

@Service
public class BookDiscoveryService {

    private static final String ISBN_PATTERN = "^(?:[0-9]{10}|[0-9]{13})$";

    private final LibraryBookRepository libraryBookRepository;
    private final BookDiscoveryClient bookDiscoveryClient;

    /**
     * bookDiscoveryClient는 실제로는 ALADIN_API_TTB_KEY를 읽는 @Lazy 빈이다. 주입 지점에도 @Lazy를 붙여야 지연 프록시가 생성되고, 그렇지 않으면 이 생성자가 즉시
     * 실행될 때 실제 빈이 만들어지며 자격 증명 placeholder가 즉시 해석된다.
     */
    BookDiscoveryService(LibraryBookRepository libraryBookRepository, @Lazy BookDiscoveryClient bookDiscoveryClient) {
        this.libraryBookRepository = libraryBookRepository;
        this.bookDiscoveryClient = bookDiscoveryClient;
    }

    public BookSearchResult search(UUID memberId, String isbn) {
        if (isbn == null || !isbn.matches(ISBN_PATTERN)) {
            throw new InvalidSearchParameterException();
        }
        return libraryBookRepository.findByMemberIdAndIsbn(memberId, isbn)
                .<BookSearchResult>map(BookSearchResult::alreadyRegistered)
                .orElseGet(() -> bookDiscoveryClient.lookup(isbn)
                        .map(BookSearchResult::found)
                        .orElseGet(BookSearchResult::notFound));
    }
}
