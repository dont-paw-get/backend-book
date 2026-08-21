package com.chc.dpgb.discovery.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.discovery.BookDiscoveryService;
import com.chc.dpgb.discovery.web.dto.BookSearchResponse;

@RestController
@RequestMapping("/api/v1/books")
public class BookDiscoveryController {

    private final BookDiscoveryService bookDiscoveryService;

    BookDiscoveryController(BookDiscoveryService bookDiscoveryService) {
        this.bookDiscoveryService = bookDiscoveryService;
    }

    @GetMapping("/search")
    public BookSearchResponse searchBookInfo(
            @RequestParam(required = false) String title, @RequestParam(required = false) String author
    ) {
        return new BookSearchResponse(bookDiscoveryService.search(title, author));
    }
}
