package com.chc.dpgb.librarian.application;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.LibrarianNotSelectedException;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.MemberLibrarianSelection;

@Service
public class LibrarianService {

    private final LibrarianRepository librarianRepository;
    private final MemberLibrarianSelectionRepository selectionRepository;

    LibrarianService(LibrarianRepository librarianRepository,
            MemberLibrarianSelectionRepository selectionRepository) {
        this.librarianRepository = librarianRepository;
        this.selectionRepository = selectionRepository;
    }

    @Transactional(readOnly = true)
    public List<Librarian> getLibrarians() {
        return librarianRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SelectedLibrarian getMyLibrarian(String memberId) {
        MemberLibrarianSelection selection = selectionRepository.findByMemberId(memberId)
                .orElseThrow(LibrarianNotSelectedException::new);
        Librarian librarian = librarianRepository.findById(selection.getLibrarianId())
                .orElseThrow(() -> new IllegalStateException(
                        "선택된 librarianId에 해당하는 사서 마스터 데이터가 없습니다: " + selection.getLibrarianId()));
        return new SelectedLibrarian(librarian, selection.getSelectedAt());
    }

    @Transactional
    public SelectedLibrarian selectMyLibrarian(String memberId, Long librarianId) {
        Librarian librarian = librarianRepository.findById(librarianId)
                .orElseThrow(LibrarianNotFoundException::new);

        Optional<MemberLibrarianSelection> existing = selectionRepository.findByMemberId(memberId);
        MemberLibrarianSelection selection;
        if (existing.isPresent()) {
            selection = existing.get();
            selection.select(librarianId);
            selection = selectionRepository.save(selection);
        } else {
            try {
                selection = selectionRepository.save(MemberLibrarianSelection.create(memberId, librarianId));
            } catch (DataIntegrityViolationException e) {
                selection = selectionRepository.findByMemberId(memberId).orElseThrow(() -> e);
                selection.select(librarianId);
                selection = selectionRepository.save(selection);
            }
        }
        return new SelectedLibrarian(librarian, selection.getSelectedAt());
    }
}
