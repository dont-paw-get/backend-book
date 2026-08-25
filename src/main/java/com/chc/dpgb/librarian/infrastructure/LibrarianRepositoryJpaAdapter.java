package com.chc.dpgb.librarian.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.chc.dpgb.librarian.application.LibrarianRepository;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;

@Repository
class LibrarianRepositoryJpaAdapter implements LibrarianRepository {

    private final LibrarianJpaRepository jpaRepository;

    LibrarianRepositoryJpaAdapter(LibrarianJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Librarian save(Librarian librarian) {
        return jpaRepository.saveAndFlush(librarian);
    }

    @Override
    public Optional<Librarian> findById(Long librarianId) {
        return jpaRepository.findById(librarianId);
    }

    @Override
    public List<Librarian> findAllOwned(UUID memberId) {
        return jpaRepository.findByMemberIdOrderByCreatedAtAsc(memberId);
    }

    @Override
    public boolean existsByMemberIdAndType(UUID memberId, LibrarianType type) {
        return jpaRepository.existsByMemberIdAndType(memberId, type);
    }

    @Override
    public Optional<Librarian> findRepresentative(UUID memberId) {
        return jpaRepository.findByMemberIdAndIsRepresentativeTrue(memberId);
    }
}
