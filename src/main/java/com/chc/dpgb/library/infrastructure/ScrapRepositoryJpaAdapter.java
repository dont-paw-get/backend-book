package com.chc.dpgb.library.infrastructure;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.chc.dpgb.library.application.ScrapRepository;
import com.chc.dpgb.library.domain.Scrap;

@Repository
class ScrapRepositoryJpaAdapter implements ScrapRepository {

    private final ScrapJpaRepository jpaRepository;

    ScrapRepositoryJpaAdapter(ScrapJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Scrap save(Scrap scrap) {
        return jpaRepository.saveAndFlush(scrap);
    }

    @Override
    public void delete(Scrap scrap) {
        jpaRepository.delete(scrap);
    }

    @Override
    public Optional<Scrap> findById(Long scrapId) {
        return jpaRepository.findById(scrapId);
    }

    @Override
    public Page<Scrap> findPageByBookId(Long bookId, Pageable pageable) {
        return jpaRepository.findByBookIdOrderByCreatedAtAsc(bookId, pageable);
    }
}
