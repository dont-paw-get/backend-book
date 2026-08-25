package com.chc.dpgb.library.infrastructure;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.library.domain.Scrap;

interface ScrapJpaRepository extends JpaRepository<Scrap, Long> {

    Page<Scrap> findByBookIdOrderByCreatedAtAsc(Long bookId, Pageable pageable);

    List<Scrap> findAllByBookId(Long bookId);
}
