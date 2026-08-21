# BACKLOG (지금 하지 않는 것)

- PostgreSQL 도입 규모가 커지면 벡터 검색 없이도 pgvector 확장을 Book Service가 쓸 일이 생길지 재검토 (현재는 RAG 서비스 전용으로 분리)
- 사용자 업로드 이미지 파일 저장(서재 책 표지 교체, 스크랩 이미지 교체)은 오브젝트 스토리지(S3 등) 연동이 필요해 CLIAR-43(ADR-0003)에서 계약 범위 밖으로 뺐다. 파일 저장을 담당할 컴포넌트/서비스가 정해지면 endpoint를 다시 설계(`replaceLibraryBookCover`, `replaceScrapImage`가 이전 설계 참고용)
- 실제 AWS Cognito User Pool이 준비되면: `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID` 실값 채우기, 진짜 발급된 Access Token으로 `SecurityConfig`의 `JwtDecoder`+`TokenUseValidator`+`ClientIdValidator` 연동 재검증(지금은 `@MockitoBean`으로 decoder를 대체한 단위/슬라이스 테스트만 있음)
- 다른 백엔드 MSA 컴포넌트가 Book Service API를 사용자 토큰 없이 M2M으로 직접 호출할 일이 생기면, Cognito Client Credentials 플로우 기반 인증을 별도로 설계(현재 `client_id` 검증은 단일 웹앱 App Client만 가정)
- 로컬 개발 시 `.env` 파일(`ALADIN_API_TTB_KEY` 등)을 앱이 자동으로 읽지 않는다 — 여러 비밀값이 늘어나 매번 셸/IDE에 수동 export하는 게 번거로워지면 dotenv 로딩 도입 여부 검토(CLIAR-34)
- `AladinBookDiscoveryClient`는 최대 10건 검색 결과로 고정되어 있고 실제 Aladin 사용량 한도(초당/일일 호출 제한)에 대한 처리(재시도, 백오프, 캐싱)가 없다 — 실제 트래픽 확인 후 필요하면 재설계
