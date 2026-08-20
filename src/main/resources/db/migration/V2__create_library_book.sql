CREATE TABLE library_book (
    book_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id      VARCHAR(255) NOT NULL,
    shelf_rank     VARCHAR(128) COLLATE "C" NOT NULL,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(100) NOT NULL,
    isbn           VARCHAR(13),
    publisher      VARCHAR(100),
    published_date DATE,
    cover_url      VARCHAR(2048),
    total_pages    INTEGER NOT NULL,
    current_page   INTEGER NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_library_book_member_shelf_rank
    ON library_book (member_id, shelf_rank);

CREATE UNIQUE INDEX uk_library_book_member_isbn
    ON library_book (member_id, isbn)
    WHERE isbn IS NOT NULL;
