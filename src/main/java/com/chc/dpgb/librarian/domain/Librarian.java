package com.chc.dpgb.librarian.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "librarian")
@SQLRestriction("deleted_at IS NULL")
@Getter
public class Librarian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long librarianId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private LibrarianType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Long experience;

    @Column(name = "is_representative", nullable = false)
    private boolean isRepresentative;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Librarian() {
    }

    private Librarian(UUID memberId, LibrarianType type, String name) {
        this.memberId = memberId;
        this.type = type;
        rename(name);
        this.level = 1;
        this.experience = 0L;
        this.isRepresentative = false;
    }

    public static Librarian acquire(UUID memberId, LibrarianType type, String name) {
        return new Librarian(memberId, type, name);
    }

    public void rename(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name은 비어 있을 수 없습니다.");
        }
        this.name = name;
    }

    public void markAsRepresentative() {
        this.isRepresentative = true;
    }

    public void unmarkAsRepresentative() {
        this.isRepresentative = false;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
