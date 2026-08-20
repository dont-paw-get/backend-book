# ADR-0007: ISBN 없는 도서의 제목·저자 기반 중복 판정 제거

- 상태: Accepted
- 일자: 2026-08-21

## 배경

`LibraryBook`의 중복 판정은 원래 "ISBN이 있으면 ISBN 우선, 없으면 정규화된 제목+저자 조합을 보조 기준으로 검토"하는 2단계 규칙이었다(`.harness/DOMAIN.md`). CLIAR-31에서 이를 구현하며 `normalizedTitle`/`normalizedAuthor` 컬럼과 `(member_id, normalized_title, normalized_author)` unique 제약까지 만들었으나, 구현 검토 중 사용자가 이 보조 기준 자체를 없애기로 확정했다 — 서로 다른 책이어도 제목과 저자가 우연히 같을 수 있는데, 서버가 이를 강제로 막으면 사용자가 정당하게 등록하려는 책을 막게 된다.

## 결정

1. ISBN이 없는 도서에 대한 중복 판정을 완전히 제거한다. 같은 사용자가 제목·저자가 같은 책을 여러 번 등록해도 막지 않는다.
2. ISBN이 있는 도서의 사용자별 유일성 판정만 남긴다 — `(member_id, isbn)` unique 제약(`isbn IS NOT NULL`인 행에만 적용)은 그대로 유지한다.
3. `LibraryBook` aggregate에서 `normalizedTitle`/`normalizedAuthor` 필드와 관련 DB 컬럼·unique 제약을 제거한다.

## 결과

- `.harness/DOMAIN.md`의 "중복" 절이 "ISBN 있으면 유일성 판정, 없으면 판정하지 않음"으로 단순화됐다.
- `src/main/resources/db/migration/V2__create_library_book.sql`에서 `normalized_title`/`normalized_author` 컬럼과 `uk_library_book_member_normalized_title_author` 인덱스를 제거했다 — 이 컬럼·제약이 아직 어떤 환경에도 배포되지 않은 시점의 결정이라 별도 마이그레이션(`ALTER TABLE ... DROP COLUMN`) 없이 `V2`를 직접 수정했다.
- `LibraryBookRepository`(포트)/`LibraryBookJpaRepository`/`LibraryBookRepositoryJpaAdapter`에서 `existsBy...NormalizedTitleAndNormalizedAuthor` 계열 메서드를 제거했다.
- `docs/api/openapi.yaml`은 애초에 이 중복 판정 메커니즘을 wire 계약 문면에 노출한 적이 없어(단지 `BookAlreadyRegistered` 409 응답만 일반적으로 서술) 스키마 변경은 없다 — 다만 ISBN 없이 같은 제목·저자로 재등록했을 때의 실제 응답이 409에서 201로 바뀌는 동작 변화가 있다.
