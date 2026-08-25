package com.chc.dpgb.librarian.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.common.exception.InvalidLibrarianDataException;
import com.chc.dpgb.librarian.application.LibrarianService;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.web.dto.AcquireLibrarianRequest;
import com.chc.dpgb.librarian.web.dto.AcquireLibrarianResponse;
import com.chc.dpgb.librarian.web.dto.LibrarianListResponse;
import com.chc.dpgb.librarian.web.dto.LibrarianTypeListResponse;
import com.chc.dpgb.librarian.web.dto.RenameLibrarianRequest;
import com.chc.dpgb.librarian.web.dto.RenameLibrarianResponse;
import com.chc.dpgb.librarian.web.dto.RepresentativeLibrarianResponse;
import com.chc.dpgb.security.MemberIdResolver;

@RestController
public class LibrarianController {

    private final LibrarianService librarianService;

    LibrarianController(LibrarianService librarianService) {
        this.librarianService = librarianService;
    }

    @GetMapping("/api/v1/librarian-types")
    public LibrarianTypeListResponse getLibrarianTypes() {
        return LibrarianTypeListResponse.from(librarianService.getLibrarianTypes());
    }

    @PostMapping("/api/v1/librarians")
    @ResponseStatus(HttpStatus.CREATED)
    public AcquireLibrarianResponse acquireLibrarian(
            @AuthenticationPrincipal Jwt jwt, @RequestBody AcquireLibrarianRequest request
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        if (request.type() == null) {
            throw new InvalidLibrarianDataException("type은 필수입니다.");
        }
        Librarian librarian = librarianService.acquireLibrarian(memberId, request.type(), request.name());
        return AcquireLibrarianResponse.from(librarian);
    }

    @GetMapping("/api/v1/librarians")
    public LibrarianListResponse getLibrarians(@AuthenticationPrincipal Jwt jwt) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        return LibrarianListResponse.from(librarianService.getLibrarians(memberId));
    }

    @PatchMapping("/api/v1/librarians/{librarianId}")
    public RenameLibrarianResponse renameLibrarian(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long librarianId,
            @RequestBody RenameLibrarianRequest request
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        Librarian librarian = librarianService.renameLibrarian(memberId, librarianId, request.name());
        return RenameLibrarianResponse.from(librarian);
    }

    @PatchMapping("/api/v1/librarians/{librarianId}/representative")
    public RepresentativeLibrarianResponse selectRepresentative(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long librarianId
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        Librarian librarian = librarianService.selectRepresentative(memberId, librarianId);
        return RepresentativeLibrarianResponse.from(librarian);
    }

    @GetMapping("/api/v1/librarians/representative")
    public RepresentativeLibrarianResponse getRepresentative(@AuthenticationPrincipal Jwt jwt) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        return RepresentativeLibrarianResponse.from(librarianService.getRepresentative(memberId));
    }

    @DeleteMapping("/api/v1/librarians/{librarianId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLibrarian(@AuthenticationPrincipal Jwt jwt, @PathVariable Long librarianId) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        librarianService.deleteLibrarian(memberId, librarianId);
    }
}
