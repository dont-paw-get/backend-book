CREATE TABLE shelf (
    shelf_id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id  VARCHAR(255) NOT NULL,
    name       VARCHAR(50) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_shelf_member_default
    ON shelf (member_id)
    WHERE is_default = true;

ALTER TABLE library_book ADD COLUMN shelf_id BIGINT;

INSERT INTO shelf (member_id, name, is_default)
SELECT DISTINCT member_id, '기본 책장', true
FROM library_book;

UPDATE library_book lb
SET shelf_id = s.shelf_id
FROM shelf s
WHERE s.member_id = lb.member_id
  AND s.is_default = true;

ALTER TABLE library_book ALTER COLUMN shelf_id SET NOT NULL;

ALTER TABLE library_book
    ADD CONSTRAINT fk_library_book_shelf FOREIGN KEY (shelf_id) REFERENCES shelf (shelf_id);

DROP INDEX uk_library_book_member_shelf_rank;

CREATE UNIQUE INDEX uk_library_book_shelf_rank
    ON library_book (shelf_id, shelf_rank);
