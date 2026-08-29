# BACKLOG (지금 하지 않는 것)

- PostgreSQL 도입 규모가 커지면 벡터 검색 없이도 pgvector 확장을 Book Service가 쓸 일이 생길지 재검토 (현재는 RAG 서비스 전용으로 분리)
- 사용자 업로드 이미지 파일 저장(서재 책 표지 교체, 스크랩 이미지 교체)은 오브젝트 스토리지(S3 등) 연동이 필요해 CLIAR-43(ADR-0003)에서 계약 범위 밖으로 뺐다. 파일 저장을 담당할 컴포넌트/서비스가 정해지면 endpoint를 다시 설계(`replaceLibraryBookCover`, `replaceScrapImage`가 이전 설계 참고용)
- 실제 AWS Cognito User Pool이 준비되면: `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID` 실값 채우기, 진짜 발급된 Access Token으로 `SecurityConfig`의 `JwtDecoder`+`TokenUseValidator`+`ClientIdValidator` 연동 재검증(지금은 `@MockitoBean`으로 decoder를 대체한 단위/슬라이스 테스트만 있음)
- 다른 백엔드 MSA 컴포넌트가 Book Service API를 사용자 토큰 없이 M2M으로 직접 호출할 일이 생기면, Cognito Client Credentials 플로우 기반 인증을 별도로 설계(현재 `client_id` 검증은 단일 웹앱 App Client만 가정)
- 로컬 개발 시 `.env` 파일(`ALADIN_API_TTB_KEY` 등)을 앱이 자동으로 읽지 않는다 — 여러 비밀값이 늘어나 매번 셸/IDE에 수동 export하는 게 번거로워지면 dotenv 로딩 도입 여부 검토(CLIAR-34)
- `AladinBookDiscoveryClient`는 실제 Aladin 사용량 한도(초당/일일 호출 제한)에 대한 처리(재시도, 백오프, 캐싱)가 없다 — 실제 트래픽 확인 후 필요하면 재설계
- 회원 탈퇴(Member 서비스에서 소프트 삭제) 시 Book Service 쪽 데이터(서재/책장/스크랩)를 어떻게 처리할지는 아직 손대지 않았다(ADR-0009 논의 중 확인) — Member 서비스에 탈퇴 이벤트/알림 인프라가 생기면 재검토(현재는 이벤트 브로커 자체가 이 시스템에 없음)
- `librarian_level`(레벨별 필요 경험치 정책) 값은 `level=1, required_experience=0` 최소 시드만 있다(ADR-0011) — 실제 레벨 구간/필요 경험치 값을 게임 기획이 정해지면 시드를 채운다.
- 사서 경험치 획득 트리거(무엇을 하면 경험치가 오르는지)와 레벨업 시점의 부수효과는 아직 설계하지 않았다(ADR-0011로 스키마/CRUD만 확정, 게임 로직은 범위 밖) — 설계되면 `LibrarianLevel` JPA 엔티티도 함께 추가한다.
- `k8s/overlays/prod/configmap-patch.yaml`의 `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID`가 dev overlay와 동일한 User Pool(`ap-northeast-2_y1mKz50El`)을 가리킨다 — 2026-08-27 prod CD 파이프라인 검증을 위해 의도적으로 공용한 것이므로, 상용 전용 User Pool이 준비되면 prod overlay만 그 값으로 교체한다.
- `createLibraryBook`의 201 응답 예시는 `genre: NONE`/`readingStatus: PLANNED` 하나뿐이라, 요청 예시 `전체_입력`(`LITERARY_FICTION`/`READING`)과 나란히 보면 "값을 보내도 기본값으로 돌아온다"처럼 읽힌다 — 요청 예시와 짝을 맞춰 응답 예시도 2종으로 나눌지 미정(사용자 확인 대기)
