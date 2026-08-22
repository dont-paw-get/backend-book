package com.chc.dpgb.librarian.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "librarian")
@Getter
public class Librarian {

    @Id
    @Column(name = "librarian_id")
    private Long librarianId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "evolution_stage", nullable = false)
    private int evolutionStage;

    protected Librarian() {
    }

    // 마스터 데이터는 Flyway 시드로만 채워지고 애플리케이션 코드가 생성하지 않는다 —
    // 이 생성자는 테스트에서 고정된 사서 데이터를 준비하는 용도로만 쓴다.
    public Librarian(Long librarianId, String name, String type, String imageUrl, int evolutionStage) {
        this.librarianId = librarianId;
        this.name = name;
        this.type = type;
        this.imageUrl = imageUrl;
        this.evolutionStage = evolutionStage;
    }
}
