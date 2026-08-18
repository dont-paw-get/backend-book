# DOMAIN (현재 업무 규칙)

이 문서는 지금 시점의 업무 규칙(aggregate가 소유하는 개념, 불변식, 상태 전이, 중복 판정)만 담는다. API wire 표현은 `docs/api/openapi.yaml`, 결정 이유는 `DECISIONS.md`, 구현 진행 상황은 `STATE.md`를 본다.

이 저장소(Book Service)가 다루는 범위는 `docs/api/openapi.yaml`에 명세된 것과 동일하다: 표지 OCR, 외부 도서 검색, AI 도서 분석, 개인 서재(LibraryBook) 관리, 독서 진도. 오늘의 기분 추천과 문장 OCR·감상·비밀 메모는 다른 MSA 컴포넌트 담당이라 이 문서의 범위 밖이다.

## Book Discovery → LibraryBook 생성 경계

- OCR·외부 검색·AI 분석 결과는 사용자가 서재 등록을 확정하기 전까지는 후보 데이터다.
- 사용자가 최종 확인해 등록을 요청하는 시점이 LibraryBook 생성 경계다.
- AI fallback을 사용했다면(`fallbackUsed`) 그 사실을 등록 시점까지 보존한다.
- 사용자가 확정한 값은 이후 OCR·AI 처리로 자동 덮어쓰지 않는다.

## LibraryBook aggregate

소유하는 개념:

- 소유자 식별자 `memberId`(요청 body가 아니라 인증 principal에서 얻는다)
- 서버 발급 식별자 `bookId`
- 사용자 서재 내 순서 `bookNumber` (등록 시점에 서버가 부여하고 등록 응답에 포함한다)
- 확인된 책 메타데이터: `title`, `author`, `isbn`, `publisher`, `publishedDate`, `genre`, `coverUrl`
- `moodTags`, `language`(ISO 639-1, 생략 시 `ko`)
- `totalPages`, `currentPage`, `ReadingStatus`
- 생성·수정 시각

모든 조회와 변경은 aggregate 소유자(`memberId`) 기준으로 수행한다.

## 페이지와 독서 상태

- `totalPages > 0`
- `0 <= currentPage <= totalPages`
- `currentPage == 0` → `NOT_STARTED`
- `0 < currentPage < totalPages` → `READING`
- `currentPage == totalPages` → `COMPLETED`
- 진도율은 서버가 `currentPage / totalPages * 100`으로 계산한다.
- 전체 페이지를 기존 현재 페이지보다 작게 줄일 수 없다.
- 이전 페이지로의 이동은 사용자의 위치 정정으로 허용한다.

## 중복

- 중복은 사용자별로 판정한다. 다른 사용자는 같은 책을 각각 등록할 수 있다.
- ISBN이 있으면 사용자별 ISBN을 우선 기준으로 사용한다.
- ISBN이 없으면 정규화한 제목과 저자 조합을 보조 기준으로 검토한다.
- 동시 등록에서도 중복이 생기지 않도록 저장소 제약(DB unique constraint)을 함께 사용한다.

## 결정된 사항 (ADR-0002 반영)

- `language` 생성 주체: 사용자가 등록·수정 시 선택 입력, 생략하면 서버가 `ko`로 채운다.
- `coverUrl`은 등록 시점뿐 아니라 이후 수정(PATCH)으로도 변경할 수 있다.
- `bookNumber`는 등록 응답 시점부터 클라이언트에 노출된다.

## 미결정 도메인

- `bookNumber`의 사용자별 유일성과 재정렬 규칙
- ISBN이 없는 책의 정확한 정규화·중복 기준
- `moodTags` 자유 문자열과 canonical mood 코드(다른 MSA 컴포넌트가 정의) 간 매핑 방식 — 필요해지면 그 컴포넌트와 함께 결정
- 표지 색상(`coverColor`)의 생성 주체와 허용 값 — 저장 필드가 정의되면 별도로 다시 설계 (ADR-0002에서 관련 필터는 우선 제거)

결정된 규칙이 API에 노출되면 `docs/api/openapi.yaml`과 계약 테스트도 함께 갱신한다.
