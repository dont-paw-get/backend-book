package com.chc.dpgb.librarian.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chc.dpgb.common.exception.InvalidLibrarianDataException;
import com.chc.dpgb.common.exception.LibrarianAccessDeniedException;
import com.chc.dpgb.common.exception.LibrarianAlreadyOwnedException;
import com.chc.dpgb.common.exception.LibrarianNotFoundException;
import com.chc.dpgb.common.exception.RepresentativeLibrarianNotSelectedException;
import com.chc.dpgb.librarian.domain.Librarian;
import com.chc.dpgb.librarian.domain.LibrarianType;
import com.chc.dpgb.librarian.domain.LibrarianTypeInfo;

@Service
public class LibrarianService {

    private static final Logger log = LoggerFactory.getLogger(LibrarianService.class);

    private final LibrarianRepository librarianRepository;
    private final LibrarianTypeInfoRepository librarianTypeInfoRepository;

    LibrarianService(
            LibrarianRepository librarianRepository, LibrarianTypeInfoRepository librarianTypeInfoRepository
    ) {
        this.librarianRepository = librarianRepository;
        this.librarianTypeInfoRepository = librarianTypeInfoRepository;
    }

    @Transactional(readOnly = true)
    public List<LibrarianTypeInfo> getLibrarianTypes() {
        return librarianTypeInfoRepository.findAll();
    }

    @Transactional
    public Librarian acquireLibrarian(UUID memberId, LibrarianType type, String name) {
        if (librarianRepository.existsByMemberIdAndType(memberId, type)) {
            throw new LibrarianAlreadyOwnedException();
        }
        Librarian librarian;
        try {
            librarian = Librarian.acquire(memberId, type, name);
        } catch (IllegalArgumentException e) {
            throw new InvalidLibrarianDataException(e.getMessage());
        }
        Librarian saved;
        try {
            saved = librarianRepository.save(librarian);
        } catch (DataIntegrityViolationException e) {
            // existsByMemberIdAndType 검사를 통과했는데도 unique 제약에 걸렸다 — 동시 획득 경합이다.
            log.warn("사서 획득 경합으로 중복 판정 type={}", type);
            throw new LibrarianAlreadyOwnedException();
        }
        log.info("사서 획득 librarianId={} type={}", saved.getLibrarianId(), type);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Librarian> getLibrarians(UUID memberId) {
        return librarianRepository.findAllOwned(memberId);
    }

    @Transactional
    public Librarian renameLibrarian(UUID memberId, Long librarianId, String name) {
        Librarian librarian = getOwnedLibrarian(memberId, librarianId);
        try {
            librarian.rename(name);
        } catch (IllegalArgumentException e) {
            throw new InvalidLibrarianDataException(e.getMessage());
        }
        return librarianRepository.save(librarian);
    }

    @Transactional
    public Librarian selectRepresentative(UUID memberId, Long librarianId) {
        Librarian target = getOwnedLibrarian(memberId, librarianId);
        librarianRepository.findRepresentative(memberId)
                .filter(current -> !current.getLibrarianId().equals(librarianId))
                .ifPresent(current -> {
                    current.unmarkAsRepresentative();
                    librarianRepository.save(current);
                    log.info(
                            "대표 사서 교체 from={} to={}",
                            current.getLibrarianId(), librarianId
                    );
                });
        target.markAsRepresentative();
        return librarianRepository.save(target);
    }

    @Transactional(readOnly = true)
    public Librarian getRepresentative(UUID memberId) {
        return librarianRepository.findRepresentative(memberId)
                .orElseThrow(RepresentativeLibrarianNotSelectedException::new);
    }

    @Transactional
    public void deleteLibrarian(UUID memberId, Long librarianId) {
        Librarian librarian = getOwnedLibrarian(memberId, librarianId);
        librarian.softDelete(Instant.now());
        librarianRepository.save(librarian);
    }

    private Librarian getOwnedLibrarian(UUID memberId, Long librarianId) {
        Librarian librarian = librarianRepository.findById(librarianId)
                .orElseThrow(LibrarianNotFoundException::new);
        if (!librarian.getMemberId().equals(memberId)) {
            throw new LibrarianAccessDeniedException();
        }
        return librarian;
    }
}
