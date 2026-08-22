package com.chc.dpgb.librarian.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.librarian.application.LibrarianService;
import com.chc.dpgb.librarian.web.dto.LibrarianListResponse;

@RestController
public class LibrarianController {

    private final LibrarianService librarianService;

    LibrarianController(LibrarianService librarianService) {
        this.librarianService = librarianService;
    }

    @GetMapping("/api/v1/librarians")
    public LibrarianListResponse getLibrarians() {
        return LibrarianListResponse.from(librarianService.getLibrarians());
    }
}
