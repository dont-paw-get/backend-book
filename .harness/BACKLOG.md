# BACKLOG (지금 하지 않는 것)

- PostgreSQL 도입 규모가 커지면 벡터 검색 없이도 pgvector 확장을 Book Service가 쓸 일이 생길지 재검토 (현재는 RAG 서비스 전용으로 분리)
- 사용자 업로드 이미지 파일 저장(서재 책 표지 교체, 스크랩 이미지 교체)은 오브젝트 스토리지(S3 등) 연동이 필요해 CLIAR-43(ADR-0003)에서 계약 범위 밖으로 뺐다. 파일 저장을 담당할 컴포넌트/서비스가 정해지면 endpoint를 다시 설계(`replaceLibraryBookCover`, `replaceScrapImage`가 이전 설계 참고용)
- Java MSA 서비스 전체가 공유하는 단일 PostgreSQL 정책(CLIAR-43, `.harness/DECISIONS.md` 참조)이 확정됐지만, 실제로 공유할 다른 서비스의 schema/테이블 이름, DB 계정·권한 분리, `docker-compose.yml`/`application-*.yaml`의 데이터베이스 이름(`dpgb`)이 공유 DB 전체를 가리키는 이름으로 바뀌어야 하는지는 다른 서비스가 구체화되는 시점에 재검토
- 실제 AWS Cognito User Pool이 준비되면: `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID` 실값 채우기, 진짜 발급된 Access Token으로 `SecurityConfig`의 `JwtDecoder`+`TokenUseValidator`+`ClientIdValidator` 연동 재검증(지금은 `@MockitoBean`으로 decoder를 대체한 단위/슬라이스 테스트만 있음)
- 다른 백엔드 MSA 컴포넌트가 Book Service API를 사용자 토큰 없이 M2M으로 직접 호출할 일이 생기면, Cognito Client Credentials 플로우 기반 인증을 별도로 설계(현재 `client_id` 검증은 단일 웹앱 App Client만 가정)
