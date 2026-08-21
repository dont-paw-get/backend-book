package com.chc.dpgb.library.domain;

import java.time.Instant;

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
@Table(name = "scrap")
@Getter
public class Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scrap_id")
    private Long scrapId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String sentence;

    @Column(name = "page_number")
    private Integer pageNumber;

    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Scrap() {
    }

    private Scrap(Long bookId, String sentence, Integer pageNumber, String memo) {
        this.bookId = bookId;
        changeSentence(sentence);
        changePageNumber(pageNumber);
        this.memo = memo;
    }

    public static Scrap create(Long bookId, String sentence, Integer pageNumber, String memo) {
        return new Scrap(bookId, sentence, pageNumber, memo);
    }

    public void update(String sentence, Integer pageNumber, String memo) {
        changeSentence(sentence);
        changePageNumber(pageNumber);
        this.memo = memo;
    }

    private void changeSentence(String sentence) {
        if (sentence == null || sentence.isBlank()) {
            throw new IllegalArgumentException("sentence는 비어 있을 수 없습니다.");
        }
        this.sentence = sentence;
    }

    private void changePageNumber(Integer pageNumber) {
        if (pageNumber != null && pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber는 1 이상이어야 합니다: " + pageNumber);
        }
        this.pageNumber = pageNumber;
    }
}
