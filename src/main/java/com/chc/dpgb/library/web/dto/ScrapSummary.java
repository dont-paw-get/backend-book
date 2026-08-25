package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.Scrap;

public record ScrapSummary(
        Long scrapId, String sentence, Integer pageNumber, String scrapImageUrl, Instant createdAt
) {

    public static ScrapSummary from(Scrap scrap) {
        return new ScrapSummary(
                scrap.getScrapId(), scrap.getSentence(), scrap.getPageNumber(), scrap.getScrapImageUrl(),
                scrap.getCreatedAt()
        );
    }
}
