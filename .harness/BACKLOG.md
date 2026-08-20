# BACKLOG (지금 하지 않는 것)

- PostgreSQL 도입 규모가 커지면 벡터 검색 없이도 pgvector 확장을 Book Service가 쓸 일이 생길지 재검토 (현재는 RAG 서비스 전용으로 분리)
- 실제 AWS Cognito User Pool이 준비되면: `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID` 실값 채우기, 진짜 발급된 Access Token으로 `SecurityConfig`의 `JwtDecoder`+`TokenUseValidator`+`ClientIdValidator` 연동 재검증(지금은 `@MockitoBean`으로 decoder를 대체한 단위/슬라이스 테스트만 있음)
- 다른 백엔드 MSA 컴포넌트가 Book Service API를 사용자 토큰 없이 M2M으로 직접 호출할 일이 생기면, Cognito Client Credentials 플로우 기반 인증을 별도로 설계(현재 `client_id` 검증은 단일 웹앱 App Client만 가정)
