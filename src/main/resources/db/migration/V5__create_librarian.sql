CREATE TABLE librarian (
    librarian_id    BIGINT NOT NULL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    image_url       VARCHAR(500) NOT NULL,
    evolution_stage INTEGER NOT NULL
);

CREATE TABLE member_librarian_selection (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    member_id    VARCHAR(255) NOT NULL,
    librarian_id BIGINT NOT NULL REFERENCES librarian (librarian_id),
    selected_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uk_member_librarian_selection_member_id
    ON member_librarian_selection (member_id);

INSERT INTO librarian (librarian_id, name, type, image_url, evolution_stage) VALUES
    (1, '러시안블루', 'CAT', 'https://example.com/librarians/cat-1.png', 1),
    (2, '슈빌', 'BIRD', 'https://example.com/librarians/bird-1.png', 1);
