package com.chc.dpgb.library.domain;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "library_book")
@SQLRestriction("deleted_at IS NULL")
@Getter
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long bookId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "shelf_id", nullable = false)
    private Long shelfId;

    @Column(name = "shelf_rank", nullable = false)
    private String shelfRank;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String isbn;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Genre genre;

    private String publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reading_status", nullable = false)
    private ReadingStatus readingStatus;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected LibraryBook() {
    }

    private LibraryBook(
            UUID memberId, Long shelfId, String shelfRank, String title, String author, String isbn,
            Genre genre, String publisher, LocalDate publishedDate, String coverUrl,
            ReadingStatus readingStatus, Integer totalPages
    ) {
        this.memberId = memberId;
        this.shelfId = shelfId;
        this.shelfRank = shelfRank;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre == null ? Genre.NONE : genre;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.coverUrl = coverUrl;
        this.readingStatus = readingStatus == null ? ReadingStatus.PLANNED : readingStatus;
        this.currentPage = 0;
        changeTotalPages(totalPages);
    }

    public static LibraryBook register(
            UUID memberId, Long shelfId, String shelfRank, String title, String author, String isbn,
            Genre genre, String publisher, LocalDate publishedDate, String coverUrl,
            ReadingStatus readingStatus, Integer totalPages
    ) {
        return new LibraryBook(
                memberId, shelfId, shelfRank, title, author, isbn, genre, publisher, publishedDate, coverUrl,
                readingStatus, totalPages
        );
    }

    public void updateMetadata(
            String title, String author, String isbn, Genre genre, String publisher, LocalDate publishedDate,
            String coverUrl, ReadingStatus readingStatus, Integer totalPages
    ) {
        if (title == null) {
            throw new IllegalArgumentException("title은 null일 수 없습니다.");
        }
        if (author == null) {
            throw new IllegalArgumentException("author는 null일 수 없습니다.");
        }
        if (genre == null) {
            throw new IllegalArgumentException("genre는 null일 수 없습니다.");
        }
        if (readingStatus == null) {
            throw new IllegalArgumentException("readingStatus는 null일 수 없습니다.");
        }
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.coverUrl = coverUrl;
        this.readingStatus = readingStatus;
        changeTotalPages(totalPages);
    }

    public void updateProgress(int currentPage, Integer totalPages) {
        changeTotalPages(totalPages);
        if (this.totalPages != null && (currentPage < 0 || currentPage > this.totalPages)) {
            throw new IllegalArgumentException(
                    "currentPage는 0 이상 totalPages(" + this.totalPages + ") 이하이어야 합니다: " + currentPage
            );
        }
        if (this.totalPages == null && currentPage < 0) {
            throw new IllegalArgumentException("currentPage는 0 이상이어야 합니다: " + currentPage);
        }
        this.currentPage = currentPage;
    }

    public void changeShelfRank(String newShelfRank) {
        this.shelfRank = newShelfRank;
    }

    public void changeShelfId(Long newShelfId, String newShelfRank) {
        this.shelfId = newShelfId;
        this.shelfRank = newShelfRank;
    }

    public Double progress() {
        if (totalPages == null) {
            return null;
        }
        return (currentPage * 100.0) / totalPages;
    }

    public void softDelete(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void changeTotalPages(Integer newTotalPages) {
        if (newTotalPages == null) {
            this.totalPages = null;
            return;
        }
        if (newTotalPages <= 0) {
            throw new IllegalArgumentException("totalPages는 0보다 커야 합니다: " + newTotalPages);
        }
        if (newTotalPages < this.currentPage) {
            throw new IllegalArgumentException(
                    "전체 페이지를 기존 현재 페이지(" + this.currentPage + ")보다 작게 줄일 수 없습니다: " + newTotalPages
            );
        }
        this.totalPages = newTotalPages;
    }
}
