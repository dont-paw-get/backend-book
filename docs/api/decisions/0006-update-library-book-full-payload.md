# ADR-0006: `updateLibraryBook`을 부분 수정에서 전체 필드 포함으로 변경

- 상태: Accepted
- 일자: 2026-08-21

## 배경

`UpdateLibraryBookRequest`는 원래 `minProperties: 1`로 임의의 부분 집합만 보내면 나머지는 기존 값을 유지하는 전형적인 partial PATCH였다. LibraryBook 도메인/영속성(CLIAR-31) 구현 리뷰 중 사용자가 이 방식 대신, 같은 저장소의 `Scrap.updateScrap`(`sentence`/`pageNumber`/`memo` 항상 전체 포함, nullable 필드는 `null`=삭제)과 동일한 방식으로 통일하기로 확정했다.

## 결정

1. `UpdateLibraryBookRequest`의 7개 필드(`title`/`author`/`isbn`/`publisher`/`publishedDate`/`coverUrl`/`totalPages`)를 모두 `required`로 바꾼다 — 부분 생략을 허용하지 않는다.
2. `title`/`author`/`totalPages`는 LibraryBook aggregate의 필수 불변값이라 `null`을 허용하지 않는다.
3. `isbn`/`publisher`/`publishedDate`/`coverUrl`는 원래도 nullable한 필드라 `null`이면 그 값을 삭제하고, 값이 있으면 교체한다(`Scrap.updateScrap`과 동일한 규칙).
4. `LibraryBook.updateMetadata(...)`(도메인 메서드)도 같은 규칙으로 맞춘다 — `null` 인자를 "생략"이 아니라 "그 필드를 지운다"는 의미로 처리한다. `title`/`author`/`totalPages`는 `null`을 받으면 그 자체가 잘못된 요청이므로 컨트롤러/서비스 계층(다음 "Library CRUD API" 티켓)이 그 시점에 400(`INVALID_BOOK_DATA`)으로 번역한다.

## 결과

- `docs/api/openapi.yaml`(v0.5.0)의 `UpdateLibraryBookRequest`가 `Scrap.updateScrap`과 같은 모양이 됐다 — 이 저장소의 PATCH endpoint 두 곳이 "부분 수정" 대 "전체 필드 포함"으로 갈리지 않고 하나의 규칙으로 통일된다.
- `.harness/DOMAIN.md`의 LibraryBook aggregate 절에 이 규칙을 반영한다.
- 클라이언트는 `updateLibraryBook` 호출 시 변경하지 않는 필드도 현재 값을 다시 채워 보내야 한다 — 기존 "누락 필드는 유지" 방식에 의존하던 클라이언트가 있다면 호환이 깨진다.
