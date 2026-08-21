package com.chc.dpgb.library;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "library_book")
@Getter
public class LibraryBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "member_id", nullable = false)
    private String memberId;

    @Column(name = "shelf_id", nullable = false)
    private Long shelfId;

    @Column(name = "shelf_rank", nullable = false)
    private String shelfRank;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private String isbn;

    private String publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;

    @Column(name = "current_page", nullable = false)
    private Integer currentPage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibraryBook() {
    }

    private LibraryBook(
            String memberId, Long shelfId, String shelfRank, String title, String author, String isbn,
            String publisher, LocalDate publishedDate, String coverUrl, int totalPages
    ) {
        this.memberId = memberId;
        this.shelfId = shelfId;
        this.shelfRank = shelfRank;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.coverUrl = coverUrl;
        this.currentPage = 0;
        changeTotalPages(totalPages);
    }

    public static LibraryBook register(
            String memberId, Long shelfId, String shelfRank, String title, String author, String isbn,
            String publisher, LocalDate publishedDate, String coverUrl, int totalPages
    ) {
        return new LibraryBook(
                memberId, shelfId, shelfRank, title, author, isbn, publisher, publishedDate, coverUrl,
                totalPages
        );
    }

    public void updateMetadata(
            String title, String author, String isbn, String publisher, LocalDate publishedDate,
            String coverUrl, int totalPages
    ) {
        if (title == null) {
            throw new IllegalArgumentException("title은 null일 수 없습니다.");
        }
        if (author == null) {
            throw new IllegalArgumentException("author는 null일 수 없습니다.");
        }
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.coverUrl = coverUrl;
        changeTotalPages(totalPages);
    }

    public void updateProgress(int currentPage, int totalPages) {
        changeTotalPages(totalPages);
        if (currentPage < 0 || currentPage > this.totalPages) {
            throw new IllegalArgumentException(
                    "currentPage는 0 이상 totalPages(" + this.totalPages + ") 이하이어야 합니다: " + currentPage
            );
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

    public double progress() {
        return (currentPage * 100.0) / totalPages;
    }

    private void changeTotalPages(int newTotalPages) {
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
