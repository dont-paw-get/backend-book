package com.chc.dpgb.library.web.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.chc.dpgb.library.Scrap;

public record ScrapPageResponse(
        List<ScrapSummary> scraps,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ScrapPageResponse from(Page<Scrap> page) {
        return new ScrapPageResponse(
                page.getContent().stream().map(ScrapSummary::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }
}
