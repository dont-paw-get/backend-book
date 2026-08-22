package com.chc.dpgb.librarian.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.librarian.domain.Librarian;

interface LibrarianJpaRepository extends JpaRepository<Librarian, Long> {
}
