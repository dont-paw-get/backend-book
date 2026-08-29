package com.chc.dpgb.discovery.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.discovery.BookDiscoveryService;
import com.chc.dpgb.discovery.web.dto.BookSearchResponse;
import com.chc.dpgb.security.MemberIdResolver;

@RestController
@RequestMapping("/api/v1/books")
public class BookDiscoveryController {

    private final BookDiscoveryService bookDiscoveryService;

    BookDiscoveryController(BookDiscoveryService bookDiscoveryService) {
        this.bookDiscoveryService = bookDiscoveryService;
    }

    @GetMapping("/search")
    public BookSearchResponse searchBookInfo(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String isbn
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        return BookSearchResponse.from(bookDiscoveryService.search(memberId, isbn));
    }
}
