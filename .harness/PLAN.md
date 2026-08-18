# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## PostgreSQL 전환

- [ ] `build.gradle`에서 `com.h2database:h2`, `spring-boot-h2console` 제거
- [ ] `org.postgresql:postgresql` (운영/개발), Testcontainers PostgreSQL 모듈 (`org.testcontainers:postgresql`, `org.testcontainers:junit-jupiter`) 추가
- [ ] `application.yaml`에 datasource(PostgreSQL) 설정과 profile 분리 추가
- [ ] Flyway 도입 여부와 최초 migration 파일 결정
- [ ] `TestcontainersConfiguration` 빈 구성 (`withReuse(true)`, `src/test/resources/testcontainers.properties`)

## Gradle 통합 테스트 태스크

- [ ] `integrationTest` source set/task 구성 (`test`와 분리)
- [ ] `check`가 `test`와 `integrationTest`를 모두 실행하도록 연결
- [ ] `RepositoryIntegrationTestSupport`(`@DataJpaTest` + Testcontainers), `IntegrationTestSupport`(`@SpringBootTest` + Testcontainers) 기반 클래스는 첫 Repository/전체 컨텍스트 테스트 작성 시점에 함께 생성

## 인증 기반

`openapi.yaml`의 모든 endpoint가 Bearer JWT 인증을 요구하므로(security: bearerAuth) 도메인 구현보다 먼저 확정한다.

- [ ] Spring Security Resource Server(JWT) 설정 — 인증 서비스가 발급한 JWT 검증(서명/만료), 별도 로그인 endpoint는 Book Service에 없음
- [ ] 인증 principal에서 `memberId`(소유자 식별자)를 얻는 공통 방법 확정 — 요청 body에 사용자 식별자를 받지 않는다는 ADR-0001 원칙 그대로
- [ ] 인증 실패(401) 시 `ErrorResponse`(`code: UNAUTHORIZED`) 포맷으로 통일 응답

## 공통 계약 인프라

- [ ] `@RestControllerAdvice` 전역 예외 처리 — `openapi.yaml`의 각 `responses.*`(400/401/403/404/409/422/502)를 `ErrorResponse{code, message}`로 매핑하는 표준 예외 계층 설계
- [ ] endpoint별 stable error code(`INVALID_BOOK_DATA`, `LIBRARY_BOOK_NOT_FOUND` 등)를 예외 타입과 1:1로 연결

## LibraryBook 도메인/영속성

`.harness/DOMAIN.md`에 정의된 업무 규칙을 구현 기준으로 삼는다.

- [ ] `LibraryBook` aggregate 설계 — `memberId`, `bookId`, `bookNumber`, 확인된 메타데이터(title/author/isbn/publisher/publishedDate/genre/coverUrl), `moodTags`, `language`, `totalPages`, `currentPage`, `ReadingStatus`, 생성/수정 시각
- [ ] 페이지·상태 불변식 구현: `totalPages > 0`, `0 <= currentPage <= totalPages`, `currentPage==0→NOT_STARTED`, `0<currentPage<totalPages→READING`, `currentPage==totalPages→COMPLETED`, 진도율 `currentPage/totalPages*100`, 전체 페이지를 기존 현재 페이지보다 작게 줄이는 것은 금지, 이전 페이지로의 이동(정정)은 허용
- [ ] 사용자별 중복 판정: ISBN이 있으면 사용자별 ISBN 우선, 없으면 정규화된 제목+저자 보조 기준, 동시 등록 대비 DB unique 제약 병행
- [ ] JPA Repository + `RepositoryIntegrationTestSupport` 기반 통합 테스트

## Library CRUD API

- [ ] `POST /api/v1/library/books`(`createLibraryBook`) — 등록, 중복 시 409
- [ ] `GET /api/v1/library/books`(`getLibraryBooks`) — moodTags/author/language/readingStatus 필터, sortBy/sortOrder, page/size 페이징 (XToMany fetch join 금지 원칙 준수)
- [ ] `GET /api/v1/library/books/{bookId}`(`getLibraryBook`) — 소유자 검증(403)·404 처리
- [ ] `PATCH /api/v1/library/books/{bookId}`(`updateLibraryBook`) — 부분 수정, 누락 필드는 기존값 유지
- [ ] `DELETE /api/v1/library/books/{bookId}`(`deleteLibraryBook`) — 204
- [ ] 각 endpoint의 `operationId`·요청/응답 스키마·status를 기준으로 MockMvc 계약 테스트 작성

## Reading Progress API

- [ ] `PATCH /api/v1/library/books/{bookId}/progress`(`updateReadingProgress`) — currentPage/totalPages 갱신, 진도율·상태 서버 계산, `currentPage > totalPages` 시 400(`INVALID_PAGE_VALUE`)

## Book Discovery API (외부 연동)

OCR·외부 도서 검색·AI 분석은 외부 시스템(S3, OCR 서비스, 외부 도서 API, Bedrock) 연동이 필요하다. 자격 증명·endpoint가 아직 없으므로 어댑터 인터페이스 + 스텁 구현으로 먼저 격리하고, 실제 연동은 자격 증명 확보 시점에 스텁을 교체한다.

- [ ] `POST /api/v1/books/ocr`(`analyzeBookCover`) — 이미지 저장(S3) + OCR 어댑터, 실패 시 422/502
- [ ] `GET /api/v1/books/search`(`searchBookInfo`) — 외부 도서 API 어댑터, 결과 없으면 200 + 빈 배열
- [ ] `POST /api/v1/books/ai-analyze`(`analyzeBookWithAI`) — Bedrock 어댑터, `fallbackUsed` 플래그 처리
- [ ] 각 어댑터는 인터페이스로 분리해 계약 테스트에서 목킹 가능하게 함

## 계약 테스트 전수화

- [ ] `openapi.yaml`의 모든 `operationId` × `responses` 조합에 대응하는 MockMvc 테스트 커버리지 확보 (ADR-0001 원칙)

