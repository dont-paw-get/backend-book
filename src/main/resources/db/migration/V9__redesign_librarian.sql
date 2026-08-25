-- librarian 소유 모델 전면 개편 (2026-08-25 DB 스키마 대개편, ADR-0009 대체)
-- 기존 librarian은 마스터 카탈로그(librarian_id 1/2, evolution_stage)였다.
-- 회원별 대표 사서 선택을 Book Service가 다시 소유하면서, librarian을
-- 회원이 실제로 보유하는 인스턴스(레벨/경험치/대표 여부)로 재정의한다.
-- 옛 마스터 카탈로그를 참조하는 다른 테이블이 없어(member_librarian_selection은 V6에서 이미 제거)
-- 안전하게 DROP 후 재생성한다. evolution_stage 개념은 새 스키마에 없어 폐기한다.
DROP TABLE librarian;

CREATE TYPE librarian_type AS ENUM (
    'RUSSIAN_BLUE',
    'SHOEBILL'
);

CREATE TABLE librarian_type_info (
    type              librarian_type PRIMARY KEY,
    image_url         TEXT NOT NULL,
    clicked_image_url TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE librarian_level (
    level               INTEGER PRIMARY KEY,
    required_experience BIGINT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_librarian_level_positive CHECK (level >= 1),
    CONSTRAINT ck_librarian_level_experience CHECK (required_experience >= 0)
);

CREATE TABLE librarian (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id         UUID NOT NULL,
    type              librarian_type NOT NULL REFERENCES librarian_type_info (type),
    name              VARCHAR(50) NOT NULL,
    level             INTEGER NOT NULL DEFAULT 1 REFERENCES librarian_level (level),
    experience        BIGINT NOT NULL DEFAULT 0,
    is_representative BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT ck_librarian_experience CHECK (experience >= 0)
);

CREATE INDEX ix_librarian_member_id
    ON librarian (member_id)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_librarian_member_type
    ON librarian (member_id, type)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX uk_librarian_member_representative
    ON librarian (member_id)
    WHERE is_representative = true AND deleted_at IS NULL;

INSERT INTO librarian_type_info (type, image_url, clicked_image_url) VALUES
    ('RUSSIAN_BLUE', 'https://example.com/librarians/cat-1.png', 'https://example.com/librarians/cat-1-clicked.png'),
    ('SHOEBILL', 'https://example.com/librarians/bird-1.png', 'https://example.com/librarians/bird-1-clicked.png');

INSERT INTO librarian_level (level, required_experience) VALUES
    (1, 0);
