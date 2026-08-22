package com.chc.dpgb.librarian.domain;

import java.time.Instant;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "member_librarian_selection")
@Getter
public class MemberLibrarianSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "librarian_id", nullable = false)
    private Long librarianId;

    @UpdateTimestamp
    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    protected MemberLibrarianSelection() {
    }

    private MemberLibrarianSelection(String memberId, Long librarianId) {
        this.memberId = memberId;
        select(librarianId);
    }

    public static MemberLibrarianSelection create(String memberId, Long librarianId) {
        return new MemberLibrarianSelection(memberId, librarianId);
    }

    public void select(Long librarianId) {
        if (librarianId == null) {
            throw new IllegalArgumentException("librarianId는 필수입니다.");
        }
        this.librarianId = librarianId;
    }
}
