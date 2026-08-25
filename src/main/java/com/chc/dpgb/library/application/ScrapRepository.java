package com.chc.dpgb.library.application;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.chc.dpgb.library.domain.Scrap;

public interface ScrapRepository {

    Scrap save(Scrap scrap);

    Optional<Scrap> findById(Long scrapId);

    Page<Scrap> findPageByBookId(Long bookId, Pageable pageable);

    List<Scrap> findAllByBookId(Long bookId);
}
