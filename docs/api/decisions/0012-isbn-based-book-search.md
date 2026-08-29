# ADR-0012: 도서 검색을 title/author에서 isbn 기준으로 전환 + 등록 여부 즉시 반환

- 상태: Accepted
- 일자: 2026-08-28

## 배경

기존 `GET /api/v1/books/search`는 사용자가 입력한 title/author로 알라딘 `ItemSearch`를 호출해 후보 목록을 반환했다. 기능 명세가 바뀌어 isbn(바코드 스캔 등으로 이미 확보한 값) 기준 단건 조회로 전환하고, 그 isbn이 로그인 사용자의 서재에 이미 등록되어 있으면 알라딘을 다시 호출하지 않고 저장된 도서 데이터를 바로 돌려줘야 한다.

## 결정

1. `GET /api/v1/books/search`의 `title`/`author` 쿼리 파라미터를 제거하고 `isbn`(필수, ISBN-10/13) 단일 파라미터로 완전히 교체한다.
2. isbn 중복 확인은 등록(`createLibraryBook`) 시점이 아니라 검색 시점에 한다 — 검색 응답이 `alreadyRegistered`를 명시하고, `true`면 `libraryBook`(저장된 도서 데이터)을, `false`면 알라딘에서 찾은 `book`(또는 둘 다 없으면 수동 입력 폴백)을 반환한다. 이미 등록된 경우 알라딘을 호출하지 않는다.
3. isbn 유일성 범위는 기존 정책(ADR-0007, 사용자별 유일 — 다른 사용자는 같은 책을 각자 등록 가능)을 그대로 유지한다. 이미 `LibraryBookRepository`와 `V2` 마이그레이션의 `(member_id, isbn)` partial unique 인덱스로 구현되어 있어 이번 변경으로 새 제약을 추가하지 않는다 — 검색 시점에 그 기존 판정을 먼저 노출할 뿐이다.
4. `createLibraryBook`의 기존 동작(중복 isbn으로 직접 등록을 시도하면 409 `BOOK_ALREADY_REGISTERED`)은 변경하지 않는다. 검색 흐름을 거치지 않고 바로 등록 API를 호출하는 경우를 위한 방어선으로 유지한다.

## 결과

- `docs/api/openapi.yaml`(v0.9.0): `searchBookInfo`의 파라미터를 `isbn`으로 교체, `BookSearchResponse`를 `books: ExternalBook[]`에서 `{ alreadyRegistered, libraryBook?, book? }` 단일 결과 구조로 재설계.
- `BookDiscoveryClient.search(title, author)` → `lookup(isbn)`로 교체, 알라딘 `ItemSearch` 대신 `ItemLookUp`(ISBN 단건 조회) 호출.
- `LibraryBookRepository.existsByIsbn` → `findByMemberIdAndIsbn`로 확장(엔티티까지 반환) — `BookDiscoveryService`가 등록 여부와 저장된 데이터를 함께 얻고, `LibraryBookService.createLibraryBook`의 기존 409 체크도 동일 메서드를 재사용한다.
- `BookDiscoveryService`가 인증된 `memberId`를 받아 서재를 먼저 조회하도록 바뀌었다 — discovery 패키지가 library 패키지(포트)를 단방향으로 참조한다(반대 방향 없음).
