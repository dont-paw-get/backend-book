# DOMAIN (현재 업무 규칙)

이 문서는 지금 시점의 업무 규칙(aggregate가 소유하는 개념, 불변식, 상태 전이, 중복 판정)만 담는다. API wire 표현은 `docs/api/openapi.yaml`, 결정 이유는 `DECISIONS.md`, 구현 진행 상황은 `STATE.md`를 본다.

이 저장소(Book Service)가 다루는 범위는 `docs/api/openapi.yaml`에 명세된 것과 동일하다: 알라딘 API 기반 외부 도서 검색, 개인 서재(LibraryBook) 관리, 독서 진도, 문장 스크랩(Scrap), 동물 사서(Librarian). 표지 이미지에서 텍스트를 추출하는 OCR과 AI 기반 도서·무드 분석, 오늘의 기분 추천은 다른 MSA 컴포넌트 담당이라 이 문서의 범위 밖이다.

**CLIAR-43에서 스크랩(Scrap)을 범위에 포함하도록 재조정했다.** 기존에는 "문장 OCR·감상·비밀 메모는 범위 밖"으로 판단했었지만(ADR-0002), 사용자가 담당 범위를 재확인하면서 문장 저장·페이지 번호·감상 및 메모 CRUD가 이 API Server 담당임을 확정했다. 문장을 이미지에서 추출하는 OCR 자체와 스크랩 이미지 업로드(오브젝트 스토리지 필요)는 여전히 범위 밖이며, 이 서비스는 이미 텍스트로 확정된 문장만 받아 저장한다. 사유는 `docs/api/decisions/0003-scope-narrowing-and-new-resources.md` 참조.

**같은 작업에서 `language` 필드도 완전히 제거했다.** ADR-0002가 도입했던 "사용자가 선택 입력, 생략 시 서버가 `ko`로 채운다"는 규칙은 더 이상 유효하지 않다 — 알라딘 API가 언어 정보를 전혀 제공하지 않고, 담당 기능표에도 언어 관리 기능이 없어 LibraryBook aggregate에서 제거했다.

## 외부 도서 검색 → LibraryBook 생성 경계

- 알라딘 API 검색 결과(또는 검색 결과가 없어 사용자가 직접 입력한 값 — 폴백)는 사용자가 서재 등록을 확정하기 전까지는 후보 데이터다.
- 사용자가 최종 확인해 등록을 요청하는 시점이 LibraryBook 생성 경계다.
- 사용자가 확정한 값은 이후 외부 도서 API 재조회로 자동 덮어쓰지 않는다.
- 알라딘 응답의 `author`는 "세네카 (지은이), 하와이 대저택 (편역)"처럼 역할 라벨이 붙은 결합 문자열이다. 검색 응답을 만드는 시점에 서버가 "(지은이)"/"(옮긴이)"/"(편역)" 등 역할 라벨을 제거하고 이름만 남긴 뒤 여럿이면 쉼표로 구분해 반환한다 — 이후 서재 저자 필터·정렬·중복 판정(정규화된 제목+저자)은 이 정리된 이름을 기준으로 한다.
- `totalPages`는 알라딘 API가 대부분의 도서에서 제공하지 않는다. 검색 결과에 없으면 사용자가 서재 등록 시 직접 입력해야 하는 것이 예외가 아니라 일반적인 경로다.

## LibraryBook aggregate

소유하는 개념:

- 소유자 식별자 `memberId`(요청 body가 아니라 인증 principal에서 얻는다)
- 서버 발급 식별자 `bookId`
- 사용자 서재 내 순서 `bookNumber` (등록 시점에 서버가 부여하고 등록 응답에 포함한다)
- 확인된 책 메타데이터: `title`, `author`, `isbn`, `publisher`, `publishedDate`, `coverUrl`
- `totalPages`, `currentPage`, `ReadingStatus`
- 생성·수정 시각

모든 조회와 변경은 aggregate 소유자(`memberId`) 기준으로 수행한다.

`coverUrl`은 등록(`createLibraryBook`)과 수정(`updateLibraryBook`) 두 경로로만 갱신된다. 값은 문자열(URL)이며, 알라딘 검색 결과의 표지 URL을 그대로 쓰거나 사용자가 다른 이미지 URL을 직접 입력한다. 사용자가 직접 촬영한 이미지 파일을 업로드해 저장(S3 등)하는 기능은 이 저장소 범위 밖이다 — 필요해지면 파일 저장을 담당할 별도 컴포넌트/서비스와 함께 재설계한다.

## Scrap aggregate

소유하는 개념:

- 소유자: 독립적으로 memberId를 갖지 않고 LibraryBook을 통해 귀속되는 하위 리소스다. 접근 권한 검증은 스크랩이 속한 LibraryBook의 소유자(`memberId`) 기준으로 한다.
- `scrapId`, `bookId`, `sentence`(필수), `pageNumber`(선택), `memo`(선택), 생성·수정 시각

불변식:

- `sentence`는 항상 값이 있어야 한다. 빈 문자열이나 삭제는 허용하지 않는다 — 문장 자체를 없애려면 스크랩을 삭제한다.
- `pageNumber`·`memo`는 선택 값이며 사용자가 추가·수정·삭제할 수 있다.
- 스크랩 수정은 `sentence`/`pageNumber`/`memo` 세 필드를 항상 모두 포함해야 하는 계약이다(부분 생략 불가). `pageNumber`·`memo`에 `null`을 보내면 그 값을 삭제하고, 값을 보내면 교체한다. `sentence`는 `null`을 허용하지 않는다.
- 스크랩에 이미지를 연결하는 기능(사진 촬영/업로드)은 이 저장소 범위 밖이다 — 파일 저장(S3 등)이 필요해 LibraryBook 표지와 같은 이유로 제외했다.

## Librarian / 대표 사서

소유하는 개념:

- `Librarian`(사서 마스터 데이터, 시스템이 관리): `librarianId`, `name`, `type`(고양이/새/곰/달팽이 등 — 종류가 늘어날 수 있어 고정 enum이 아니라 서버가 관리하는 문자열), `imageUrl`, `evolutionStage`
- 회원별 대표 사서 선택: `memberId` → `librarianId`, 선택 시각

불변식:

- 회원은 대표 사서를 최대 1개만 가질 수 있고, 언제든 다른 사서로 변경할 수 있다 — 새 선택이 이전 선택을 덮어쓴다.
- 존재하지 않는 `librarianId`는 선택할 수 없다(404).
- 아직 대표 사서를 선택하지 않은 회원의 조회는 404로 응답한다 — 선택을 강제로 기본값 지정하지 않고 미선택 상태를 명시적으로 구분한다.

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

- `coverUrl`은 등록 시점뿐 아니라 이후 수정(PATCH)으로도 변경할 수 있다.
- `bookNumber`는 등록 응답 시점부터 클라이언트에 노출된다.

## 미결정 도메인

- `bookNumber`의 사용자별 유일성과 재정렬 규칙
- ISBN이 없는 책의 정확한 정규화·중복 기준
- 표지 색상(`coverColor`)의 생성 주체와 허용 값 — 저장 필드가 정의되면 별도로 다시 설계 (ADR-0002에서 관련 필터는 우선 제거)
- 스크랩 목록 조회의 정렬 기준(현재는 서버 기본 정렬만 있고 클라이언트가 선택할 수 있는 sortBy가 없음) — 필요해지면 Library 목록과 같은 방식으로 추가

결정된 규칙이 API에 노출되면 `docs/api/openapi.yaml`과 계약 테스트도 함께 갱신한다.
