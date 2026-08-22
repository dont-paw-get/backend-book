package com.chc.dpgb.librarian.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.librarian.domain.Librarian;

@Service
public class LibrarianService {

    private final LibrarianRepository librarianRepository;

    LibrarianService(LibrarianRepository librarianRepository) {
        this.librarianRepository = librarianRepository;
    }

    @Transactional(readOnly = true)
    public List<Librarian> getLibrarians() {
        return librarianRepository.findAll();
    }
}
