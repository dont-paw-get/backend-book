package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.Scrap;

public record ScrapSummary(Long scrapId, String sentence, Integer pageNumber, Instant createdAt) {

    public static ScrapSummary from(Scrap scrap) {
        return new ScrapSummary(scrap.getScrapId(), scrap.getSentence(), scrap.getPageNumber(), scrap.getCreatedAt());
    }
}
