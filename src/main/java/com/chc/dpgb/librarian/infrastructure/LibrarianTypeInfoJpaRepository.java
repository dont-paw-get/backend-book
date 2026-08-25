package com.chc.dpgb.librarian.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

interface LibrarianTypeInfoJpaRepository extends JpaRepository<LibrarianTypeInfo, LibrarianType> {
}
