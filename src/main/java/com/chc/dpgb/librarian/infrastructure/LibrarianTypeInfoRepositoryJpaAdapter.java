package com.chc.dpgb.librarian.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.chc.dpgb.librarian.application.LibrarianTypeInfoRepository;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

@Repository
class LibrarianTypeInfoRepositoryJpaAdapter implements LibrarianTypeInfoRepository {

    private final LibrarianTypeInfoJpaRepository jpaRepository;

    LibrarianTypeInfoRepositoryJpaAdapter(LibrarianTypeInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<LibrarianTypeInfo> findAll() {
        return jpaRepository.findAll();
    }
}
