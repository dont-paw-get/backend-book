package com.chc.dpgb.library.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.library.application.LibraryBookService;
import com.chc.dpgb.library.application.LibrarySortBy;
import com.chc.dpgb.library.application.ShelfService;
import com.chc.dpgb.library.domain.LibraryBook;
import com.chc.dpgb.library.domain.Shelf;
import com.chc.dpgb.library.web.dto.CreateShelfRequest;
import com.chc.dpgb.library.web.dto.CreateShelfResponse;
import com.chc.dpgb.library.web.dto.LibraryBookPageResponse;
import com.chc.dpgb.library.web.dto.ShelfListResponse;
import com.chc.dpgb.library.web.dto.ShelfSummary;
import com.chc.dpgb.library.web.dto.UpdateShelfRequest;
import com.chc.dpgb.library.web.dto.UpdateShelfResponse;
import com.chc.dpgb.security.MemberIdResolver;

@RestController
@RequestMapping("/api/v1/library/shelves")
public class ShelfController {

    private final ShelfService shelfService;
    private final LibraryBookService libraryBookService;

    ShelfController(ShelfService shelfService, LibraryBookService libraryBookService) {
        this.shelfService = shelfService;
        this.libraryBookService = libraryBookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateShelfResponse createShelf(
            @AuthenticationPrincipal Jwt jwt, @RequestBody CreateShelfRequest request
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        Shelf shelf = shelfService.createShelf(memberId, request.name());
        return CreateShelfResponse.from(shelf);
    }

    @GetMapping
    public ShelfListResponse getShelves(@AuthenticationPrincipal Jwt jwt) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        List<Shelf> shelves = shelfService.getShelves(memberId);
        List<ShelfSummary> summaries = shelves.stream()
                .map(shelf -> ShelfSummary.of(shelf, shelfService.bookCount(shelf.getShelfId())))
                .toList();
        return new ShelfListResponse(summaries);
    }

    @PatchMapping("/{shelfId}")
    public UpdateShelfResponse updateShelf(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long shelfId,
            @RequestBody UpdateShelfRequest request
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        Shelf shelf = shelfService.updateShelf(memberId, shelfId, request.name());
        return UpdateShelfResponse.from(shelf);
    }

    @DeleteMapping("/{shelfId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShelf(@AuthenticationPrincipal Jwt jwt, @PathVariable Long shelfId) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        shelfService.deleteShelf(memberId, shelfId);
    }

    @GetMapping("/{shelfId}/books")
    public LibraryBookPageResponse getShelfBooks(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long shelfId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        UUID memberId = MemberIdResolver.resolve(jwt);
        shelfService.getOwnedShelf(memberId, shelfId);
        LibraryBookController.validatePaging(page, size);
        Page<LibraryBook> result = libraryBookService.getLibraryBooks(
                memberId, shelfId, null, LibrarySortBy.SHELF_ORDER, Sort.Direction.ASC, page, size
        );
        return LibraryBookPageResponse.from(result);
    }
}
