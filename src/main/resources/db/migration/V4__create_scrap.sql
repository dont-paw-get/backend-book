CREATE TABLE scrap (
    scrap_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id     BIGINT NOT NULL REFERENCES library_book (book_id) ON DELETE CASCADE,
    sentence    VARCHAR(2000) NOT NULL,
    page_number INTEGER,
    memo        VARCHAR(2000),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_scrap_book_id ON scrap (book_id);
