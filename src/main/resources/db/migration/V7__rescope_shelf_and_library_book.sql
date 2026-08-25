-- shelf: PK 리네이밍, member_id UUID화, soft delete 도입 (2026-08-25 DB 스키마 대개편)
ALTER TABLE shelf RENAME COLUMN shelf_id TO id;
ALTER TABLE shelf ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE shelf ALTER COLUMN member_id TYPE UUID USING member_id::uuid;

CREATE INDEX ix_shelf_member_id ON shelf (member_id);

DROP INDEX uk_shelf_member_default;
CREATE UNIQUE INDEX uk_shelf_member_default
    ON shelf (member_id)
    WHERE is_default = true AND deleted_at IS NULL;

-- library_book: PK 리네이밍, member_id UUID화, soft delete 도입,
-- genre/reading_status 재도입(ADR-0003 genre 부분 반전), total_pages nullable화, cover_url TEXT화
ALTER TABLE library_book RENAME COLUMN book_id TO id;
ALTER TABLE library_book ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE library_book ALTER COLUMN member_id TYPE UUID USING member_id::uuid;
ALTER TABLE library_book ALTER COLUMN total_pages DROP NOT NULL;
ALTER TABLE library_book ALTER COLUMN cover_url TYPE TEXT;
ALTER TABLE library_book ALTER COLUMN current_page SET DEFAULT 0;

CREATE TYPE genre_type AS ENUM (
    'NONE',
    'SCIENCE_FICTION',
    'FANTASY',
    'ROMANCE',
    'MYSTERY_THRILLER',
    'LITERARY_FICTION',
    'ESSAY',
    'POETRY_DRAMA',
    'HUMANITIES',
    'HISTORY',
    'BUSINESS_ECONOMICS',
    'SELF_HELP',
    'SCIENCE',
    'ARTS',
    'RELIGION',
    'COMPUTER_IT'
);

CREATE TYPE book_reading_status AS ENUM (
    'PLANNED',
    'READING',
    'COMPLETED'
);

ALTER TABLE library_book ADD COLUMN genre genre_type NOT NULL DEFAULT 'NONE';
ALTER TABLE library_book ADD COLUMN reading_status book_reading_status NOT NULL DEFAULT 'PLANNED';

DROP INDEX uk_library_book_shelf_rank;
CREATE UNIQUE INDEX uk_library_book_shelf_rank
    ON library_book (shelf_id, shelf_rank)
    WHERE deleted_at IS NULL;

DROP INDEX uk_library_book_member_isbn;
CREATE UNIQUE INDEX uk_library_book_member_isbn
    ON library_book (member_id, isbn)
    WHERE isbn IS NOT NULL AND deleted_at IS NULL;
