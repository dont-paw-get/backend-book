package com.chc.dpgb.library.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.chc.dpgb.library.application.ScrapService;
import com.chc.dpgb.library.domain.Scrap;
import com.chc.dpgb.library.web.dto.CreateScrapRequest;
import com.chc.dpgb.library.web.dto.ScrapDetailResponse;
import com.chc.dpgb.library.web.dto.ScrapPageResponse;
import com.chc.dpgb.library.web.dto.UpdateScrapRequest;
import com.chc.dpgb.security.MemberIdResolver;

@RestController
public class ScrapController {

    private final ScrapService scrapService;

    ScrapController(ScrapService scrapService) {
        this.scrapService = scrapService;
    }

    @PostMapping("/api/v1/library/books/{bookId}/scraps")
    @ResponseStatus(HttpStatus.CREATED)
    public ScrapDetailResponse createScrap(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
            @RequestBody CreateScrapRequest request
    ) {
        String memberId = MemberIdResolver.resolve(jwt);
        Scrap scrap = scrapService.createScrap(memberId, bookId, request.sentence(), request.pageNumber(),
                request.memo());
        return ScrapDetailResponse.from(scrap);
    }

    @GetMapping("/api/v1/library/books/{bookId}/scraps")
    public ScrapPageResponse getScraps(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long bookId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        String memberId = MemberIdResolver.resolve(jwt);
        LibraryBookController.validatePaging(page, size);
        Page<Scrap> result = scrapService.getScraps(memberId, bookId, page, size);
        return ScrapPageResponse.from(result);
    }

    @GetMapping("/api/v1/library/scraps/{scrapId}")
    public ScrapDetailResponse getScrap(@AuthenticationPrincipal Jwt jwt, @PathVariable Long scrapId) {
        String memberId = MemberIdResolver.resolve(jwt);
        return ScrapDetailResponse.from(scrapService.getScrap(memberId, scrapId));
    }

    @PatchMapping("/api/v1/library/scraps/{scrapId}")
    public ScrapDetailResponse updateScrap(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long scrapId,
            @RequestBody UpdateScrapRequest request
    ) {
        String memberId = MemberIdResolver.resolve(jwt);
        Scrap scrap = scrapService.updateScrap(memberId, scrapId, request.sentence(), request.pageNumber(),
                request.memo());
        return ScrapDetailResponse.from(scrap);
    }

    @DeleteMapping("/api/v1/library/scraps/{scrapId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteScrap(@AuthenticationPrincipal Jwt jwt, @PathVariable Long scrapId) {
        String memberId = MemberIdResolver.resolve(jwt);
        scrapService.deleteScrap(memberId, scrapId);
    }
}
