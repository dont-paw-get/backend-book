package com.chc.dpgb.library.web.dto;

import java.time.Instant;

import com.chc.dpgb.library.domain.Scrap;

public record ScrapDetailResponse(
        Long scrapId,
        Long bookId,
        String sentence,
        Integer pageNumber,
        String scrapImageUrl,
        String memo,
        Instant createdAt,
        Instant updatedAt
) {

    public static ScrapDetailResponse from(Scrap scrap) {
        return new ScrapDetailResponse(
                scrap.getScrapId(), scrap.getBookId(), scrap.getSentence(), scrap.getPageNumber(),
                scrap.getScrapImageUrl(), scrap.getMemo(), scrap.getCreatedAt(), scrap.getUpdatedAt()
        );
    }
}
