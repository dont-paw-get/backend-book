package com.chc.dpgb.librarian.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "librarian_type_info")
@Getter
public class LibrarianTypeInfo {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private LibrarianType type;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "clicked_image_url", nullable = false)
    private String clickedImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LibrarianTypeInfo() {
    }

    // 마스터 데이터는 Flyway 시드로만 채워지고 애플리케이션 코드가 생성하지 않는다 —
    // 이 생성자는 테스트에서 고정된 데이터를 준비하는 용도로만 쓴다.
    public LibrarianTypeInfo(LibrarianType type, String imageUrl, String clickedImageUrl) {
        this.type = type;
        this.imageUrl = imageUrl;
        this.clickedImageUrl = clickedImageUrl;
    }
}
