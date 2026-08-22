package com.chc.dpgb.librarian.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.librarian.application.LibrarianService;
import com.chc.dpgb.librarian.application.SelectedLibrarian;
import com.chc.dpgb.librarian.web.dto.LibrarianListResponse;
import com.chc.dpgb.librarian.web.dto.MyLibrarianResponse;
import com.chc.dpgb.librarian.web.dto.SelectLibrarianRequest;
import com.chc.dpgb.security.MemberIdResolver;

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

    @GetMapping("/api/v1/members/me/librarian")
    public MyLibrarianResponse getMyLibrarian(@AuthenticationPrincipal Jwt jwt) {
        String memberId = MemberIdResolver.resolve(jwt);
        SelectedLibrarian selected = librarianService.getMyLibrarian(memberId);
        return MyLibrarianResponse.from(selected);
    }

    @PutMapping("/api/v1/members/me/librarian")
    public MyLibrarianResponse selectMyLibrarian(
            @AuthenticationPrincipal Jwt jwt, @RequestBody SelectLibrarianRequest request
    ) {
        String memberId = MemberIdResolver.resolve(jwt);
        SelectedLibrarian selected = librarianService.selectMyLibrarian(memberId, request.librarianId());
        return MyLibrarianResponse.from(selected);
    }
}
