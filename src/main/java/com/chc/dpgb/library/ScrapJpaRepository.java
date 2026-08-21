package com.chc.dpgb.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScrapJpaRepository extends JpaRepository<Scrap, Long> {

    Page<Scrap> findByBookIdOrderByCreatedAtAsc(Long bookId, Pageable pageable);
}
