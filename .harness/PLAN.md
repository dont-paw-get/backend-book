# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## 인증 기반 (CLIAR-28)

`openapi.yaml`의 모든 endpoint가 Bearer JWT 인증을 요구하므로(security: bearerAuth) 도메인 구현보다 먼저 확정한다.

인증 서비스는 AWS Cognito(User Pool)로 확정. Book Service는 웹앱 하나(모바일도 웹뷰로 동일 웹앱)에서만 호출되므로 App Client는 사실상 1개 — client_id 검증까지 지금 같이 처리한다.

- **issuer-uri 형식**: `https://cognito-idp.{region}.amazonaws.com/{userPoolId}` — 실제 값은 아직 없어 `AUTH_ISSUER_URI` env var로만 구성(기본값 없음, prod/local 동일)
- **Cognito Access Token 특성**: `aud` 클레임이 없어 Spring의 `audiences` 검증 옵션은 사용하지 않는다. 대신 `token_use` 클레임으로 access token만 허용(ID Token 거부)하고, `client_id` 클레임으로 등록된 웹앱 App Client가 발급한 토큰인지 검증한다(`AUTH_APP_CLIENT_ID` env var, 기본값 없음).
- **memberId**: Cognito `sub`는 불변 UUID라 회원 식별자로 적합 — `sub` 클레임 사용, 추출은 한 곳에 모은 유틸(`MemberIdResolver`)로 분리

- [ ] `build.gradle`에 `spring-boot-starter-oauth2-resource-server`(implementation), `spring-security-test`(testImplementation) 추가
- [ ] `application.yaml`에 `spring.security.oauth2.resourceserver.jwt.issuer-uri: ${AUTH_ISSUER_URI}`, 커스텀 프로퍼티 `book-service.security.cognito.app-client-id: ${AUTH_APP_CLIENT_ID}` 추가 (기본값 없음, prod/local 모두 env var로 주입)
- [ ] `com.chc.dpgb.security.jwt.TokenUseValidator`, `ClientIdValidator`(둘 다 `OAuth2TokenValidator<Jwt>`) 구현 — 순수 함수라 단위 테스트로 결과(성공/실패) 검증
- [ ] `com.chc.dpgb.security.SecurityConfig`: `JwtDecoder` 빈(issuer-uri 기반 + 위 두 validator를 `DelegatingOAuth2TokenValidator`로 결합), `SecurityFilterChain` 빈(모든 요청 인증 필수, stateless, CSRF 비활성화), `AuthenticationEntryPoint` 빈을 한 곳에서 구성
- [ ] `com.chc.dpgb.security.MemberIdResolver`: `Jwt`에서 `sub` 클레임을 memberId로 추출하는 단일 지점 유틸 — 클레임 이름이 바뀌어도 이 한 곳만 수정
- [ ] `com.chc.dpgb.security.JwtAuthenticationEntryPoint` + `com.chc.dpgb.common.ErrorResponse`: 인증 실패(401) 시 `ErrorResponse{code: UNAUTHORIZED, message: "인증이 필요합니다."}` JSON 응답 통일 — `@RestControllerAdvice`(공통 계약 인프라, 별도 섹션)가 아직 없으므로 이 EntryPoint는 최소 구현으로 두고, 공통 계약 인프라 작업 시 예외 계층과의 통합 여부 재검토
- [ ] 검증 테스트: `TokenUseValidatorTest`/`ClientIdValidatorTest`(단위, 성공/실패 케이스), `MemberIdResolverTest`(단위), 아직 도메인 컨트롤러가 없으므로 테스트 클래스 안에 테스트 전용 `@RestController`를 두고 `@WebMvcTest` + `SecurityConfig` import로 (a) 토큰 없는 요청 401(`ErrorResponse` 포맷), (b) `spring-security-test`의 `jwt()` post-processor로 인증된 요청 200 + memberId 추출 확인. `JwtDecoder`는 `@MockitoBean`으로 대체해 실제 Cognito 네트워크 호출 없이 검증(issuer-uri 미설정 상태에서도 테스트 가능). 프로덕션 코드에는 테스트용 컨트롤러를 남기지 않는다. validator와 decoder의 실제 연동(발급된 진짜 Cognito 토큰 검증)은 실제 User Pool이 준비되기 전까지는 검증할 수 없음 — 이후 재검토.

## 공통 계약 인프라

- [ ] `@RestControllerAdvice` 전역 예외 처리 — `openapi.yaml`의 각 `responses.*`(400/401/403/404/409/422/502)를 `ErrorResponse{code, message}`로 매핑하는 표준 예외 계층 설계
- [ ] endpoint별 stable error code(`INVALID_BOOK_DATA`, `LIBRARY_BOOK_NOT_FOUND` 등)를 예외 타입과 1:1로 연결

## LibraryBook 도메인/영속성

`.harness/DOMAIN.md`에 정의된 업무 규칙을 구현 기준으로 삼는다.

- [ ] `LibraryBook` aggregate 설계 — `memberId`, `bookId`, `bookNumber`, 확인된 메타데이터(title/author/isbn/publisher/publishedDate/genre/coverUrl), `moodTags`, `language`, `totalPages`, `currentPage`, `ReadingStatus`, 생성/수정 시각
- [ ] 페이지·상태 불변식 구현: `totalPages > 0`, `0 <= currentPage <= totalPages`, `currentPage==0→NOT_STARTED`, `0<currentPage<totalPages→READING`, `currentPage==totalPages→COMPLETED`, 진도율 `currentPage/totalPages*100`, 전체 페이지를 기존 현재 페이지보다 작게 줄이는 것은 금지, 이전 페이지로의 이동(정정)은 허용
- [ ] 사용자별 중복 판정: ISBN이 있으면 사용자별 ISBN 우선, 없으면 정규화된 제목+저자 보조 기준, 동시 등록 대비 DB unique 제약 병행
- [ ] JPA Repository + `RepositoryIntegrationTestSupport`(`@DataJpaTest` + Testcontainers, 아직 미생성 — 첫 Repository 테스트 작성 시 신설) 기반 통합 테스트

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

