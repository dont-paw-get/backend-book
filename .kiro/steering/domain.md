---
inclusion: fileMatch
fileMatchPattern:
  - "src/**/*.java"
  - "docs/api/openapi.yaml"
---
# Book Service 도메인 규칙

이 문서는 Book Service의 **aggregate, 값, 상태 전이, 업무 불변식**만 소유한다. HTTP 표현은 OpenAPI, 저장 기술은 `java-spring.md`, 서비스 경계는 `architecture.md`를 따른다.

## Book Discovery
- OCR·외부 검색·AI 분석 결과는 개인 서재에 등록되기 전까지 후보 데이터다.
- 사용자가 최종 확인해 등록을 요청하는 시점이 `LibraryBook` 생성 경계다.
- AI fallback을 사용했다면 결과에 그 사실을 보존한다.
- 사용자 확정값은 후속 OCR·AI 처리로 자동 덮어쓰지 않는다.

## LibraryBook aggregate
소유하는 개념:
- 소유자 식별자 `memberId`
- 서버 발급 식별자 `bookId`
- 사용자 서재 내 순서 `bookNumber`
- 확인된 책 메타데이터와 표지 위치
- 무드 태그, 언어, 표지 색상
- `totalPages`, `currentPage`, `ReadingStatus`
- 생성·수정 시각

모든 조회와 변경은 aggregate 소유자를 기준으로 수행한다.

## 페이지와 독서 상태
- `totalPages > 0`
- `0 <= currentPage <= totalPages`
- `currentPage == 0`이면 `NOT_STARTED`
- `0 < currentPage < totalPages`이면 `READING`
- `currentPage == totalPages`이면 `COMPLETED`
- 진도율은 서버가 `currentPage / totalPages * 100`으로 계산한다.
- 전체 페이지를 기존 현재 페이지보다 작게 줄일 수 없다.
- 이전 페이지로의 이동은 사용자의 위치 정정으로 허용한다.

## 중복
- 중복은 사용자별로 판정한다. 다른 사용자는 같은 책을 각각 등록할 수 있다.
- ISBN이 있으면 사용자별 ISBN을 우선 기준으로 사용한다.
- ISBN이 없으면 정규화한 제목과 저자 조합을 보조 기준으로 검토한다.
- 동시 등록에서도 중복이 생기지 않도록 저장소 제약을 함께 사용한다.

## 미결정 도메인
- `bookNumber`의 사용자별 유일성과 재정렬 규칙
- ISBN이 없는 책의 정확한 정규화·중복 기준
- `product.md`의 무드 후보를 저장용 canonical code와 API 값으로 매핑하는 방식
- 언어·표지 색상의 생성 주체와 허용 값
- 진도율의 저장 여부와 반올림 자릿수

결정된 규칙이 API에 노출되면 OpenAPI와 계약 테스트도 함께 갱신한다.
