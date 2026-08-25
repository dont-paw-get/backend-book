-- scrap: PK 리네이밍, soft delete 도입, scrap_image_url 필수 필드 신설,
-- book_id FK의 ON DELETE CASCADE 제거(soft delete 체계에서는 애플리케이션이 명시적으로 처리)
-- (2026-08-25 DB 스키마 대개편)
ALTER TABLE scrap RENAME COLUMN scrap_id TO id;
ALTER TABLE scrap ADD COLUMN deleted_at TIMESTAMPTZ;

ALTER TABLE scrap ALTER COLUMN sentence TYPE TEXT;
ALTER TABLE scrap ALTER COLUMN memo TYPE TEXT;

ALTER TABLE scrap ADD COLUMN scrap_image_url TEXT NOT NULL DEFAULT '';
ALTER TABLE scrap ALTER COLUMN scrap_image_url DROP DEFAULT;

ALTER TABLE scrap DROP CONSTRAINT scrap_book_id_fkey;
ALTER TABLE scrap ADD CONSTRAINT fk_scrap_book FOREIGN KEY (book_id) REFERENCES library_book (id);
