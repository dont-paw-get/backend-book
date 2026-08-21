package com.chc.dpgb.library;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScrapRepository {

    Scrap save(Scrap scrap);

    void delete(Scrap scrap);

    Optional<Scrap> findById(Long scrapId);

    Page<Scrap> findPageByBookId(Long bookId, Pageable pageable);
}
