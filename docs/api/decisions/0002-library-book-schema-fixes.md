# ADR-0002: 서재 도서 스키마 불일치 수정

- 상태: Accepted
- 일자: 2026-08-18

## 배경
CLIAR-10에서 `docs/api/openapi.yaml`을 삭제된 `.kiro/steering/{product,domain}.md`의 제품·도메인 기준과 대조한 결과, 서재(Library) 관련 스키마에 다음 불일치가 발견됐다.

1. `GET /api/v1/library/books`의 `coverColors` 필터는 어떤 요청/응답 스키마에도 대응 저장 필드가 없어 값을 채울 방법이 없었다.
2. `language`는 목록 필터와 `LibraryBookSummary` 응답에만 있고, 등록(`CreateLibraryBookRequest`)·수정(`UpdateLibraryBookRequest`)·상세 조회(`LibraryBookDetailResponse`)에는 없어 값이 어떻게 채워지는지 계약상 알 수 없었다.
3. `coverUrl`은 등록 요청과 외부 검색·AI 분석 결과에는 있지만 서재 조회 응답(`LibraryBookSummary`, `LibraryBookDetailResponse`)에는 없었다. 표지 진열이 핵심인 제품 특성과 맞지 않는다.
4. `bookNumber`는 상세 조회와 수정에는 있지만 등록 응답(`CreateLibraryBookResponse`)에는 없어, 등록 시점에 서버가 부여한 번호를 클라이언트가 알 방법이 없었다.

## 결정
1. `coverColors` 쿼리 파라미터를 제거한다. 표지 색상 필터는 저장 필드가 정의되는 시점에 별도로 다시 추가한다.
2. `language`를 `CreateLibraryBookRequest`(선택, 미입력 시 서버가 `ko`로 채움), `UpdateLibraryBookRequest`(선택), `LibraryBookDetailResponse`(필수)에 추가한다.
3. `coverUrl`을 `LibraryBookSummary`, `LibraryBookDetailResponse`에 선택 필드로 추가한다.
4. `bookNumber`를 `CreateLibraryBookResponse`에 필수 필드로 추가한다.

## 결과
- 서재 도서의 언어·표지·서재 내 순번이 등록부터 조회까지 계약상 일관되게 노출된다.
- 값을 채울 방법이 없던 `coverColors` 필터가 계약에서 제거돼 클라이언트가 오해할 여지가 줄어든다.
- 오늘의 기분 추천, 문장 OCR·감상·비밀 메모는 이 저장소(Book Service) 범위 밖(다른 MSA 컴포넌트 담당)으로 확인되어 이번 수정 대상에서 제외했다.
