package com.chc.dpgb.librarian.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.chc.dpgb.librarian.application.LibrarianRepository;
import com.chc.dpgb.librarian.domain.Librarian;

@Repository
class LibrarianRepositoryJpaAdapter implements LibrarianRepository {

    private final LibrarianJpaRepository jpaRepository;

    LibrarianRepositoryJpaAdapter(LibrarianJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Librarian> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Librarian> findById(Long librarianId) {
        return jpaRepository.findById(librarianId);
    }
}
