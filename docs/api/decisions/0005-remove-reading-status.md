# ADR-0005: `ReadingStatus` 제거

- 상태: Accepted
- 일자: 2026-08-20

## 배경

`LibraryBook`은 `currentPage`/`totalPages`로부터 계산되는 `ReadingStatus`(`NOT_STARTED`/`READING`/`COMPLETED`)를 별도 필드로 노출하고 있었다(`getLibraryBooks`의 `readingStatus` 필터, `UpdateReadingProgressResponse.readingStatus`). LibraryBook 도메인/영속성(CLIAR-31) 구현을 시작하는 시점에 사용자가 이 개념 자체가 필요 없다고 확정했다.

## 결정

`ReadingStatus`를 계약과 도메인 규칙에서 완전히 제거한다.

1. `docs/api/openapi.yaml`에서 `ReadingStatus` 스키마, `getLibraryBooks`의 `readingStatus` 필터 파라미터, `UpdateReadingProgressResponse.readingStatus` 필드를 삭제한다. `updateReadingProgress`는 `progress`(진도율)만 계산해 반환한다.
2. `currentPage`/`totalPages`로부터 상태를 유도하는 규칙(`0`→`NOT_STARTED`, 중간→`READING`, `totalPages`와 같음→`COMPLETED`)은 더 이상 도메인 규칙이 아니다. 진도율(`currentPage / totalPages * 100`) 계산과 페이지 불변식(`totalPages > 0`, `0 <= currentPage <= totalPages`, 전체 페이지를 기존 현재 페이지보다 작게 줄이는 것 금지, 이전 페이지로의 정정 허용)만 유지한다.

## 결과

- `.harness/DOMAIN.md`의 LibraryBook aggregate 소유 개념에서 `ReadingStatus`를 제거하고 "페이지와 독서 상태" 절을 "페이지와 진도율"로 다시 정리했다.
- `.harness/PLAN.md`(LibraryBook 도메인/영속성, Library CRUD API)에서 `ReadingStatus` enum·필터·응답 필드 관련 체크리스트 항목을 제거했다.
- CLIAR-31 도메인/영속성 구현이 아직 코드로 존재하지 않는 시점에 결정되어, 별도의 마이그레이션/리팩터링 없이 처음부터 `ReadingStatus` 없이 설계·구현한다.

## 이후 반전됨

**이 ADR 전체가 ADR-0010으로 반전됐다** — `readingStatus`(값 구성은 `PLANNED`/`READING`/`COMPLETED`로 이 ADR 당시의 `NOT_STARTED`/`READING`/`COMPLETED`와 다름)가 `library_book`에 다시 도입됐다. 다만 `currentPage`/`totalPages`로부터 상태를 자동 유도하는 규칙(2번)은 부활하지 않았다 — 여전히 클라이언트가 명시적으로 설정하는 독립 필드다.
