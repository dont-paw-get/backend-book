# STATE (완료 스냅샷)

단계가 끝나면 그 단계를 한 줄로 갱신한다. 세션별 서술은 `HANDOFF.md`에 남긴다.

## 완료된 단계

- 프로젝트 골격: Spring Boot 4.1.0 / Java 21 애플리케이션 초기 생성, `DpgbApplicationTests` smoke test 존재.
- API 계약 정규화: `docs/api/openapi.yaml` 및 ADR-0001(`docs/api/decisions/0001-contract-normalization.md`) 수립.
- 개발 하네스 전환: 기존 `.kiro/steering` 산출물을 삭제하고 `AGENTS.md` + `.harness/*` 체계로 통합. DB는 PostgreSQL 단일 기준으로 확정, H2 제거 결정.
- API 계약 보완(CLIAR-10): `docs/api/openapi.yaml`에서 대응 필드 없는 `coverColors` 필터 제거, `language`/`coverUrl`/`bookNumber` 스키마 불일치 수정(ADR-0002). `docs/api/README.md`의 삭제된 `.kiro/steering` 문서 참조를 `AGENTS.md`/`.harness/ARCHITECTURE.md`로 정리. 오늘의 기분 추천·문장 OCR/감상/비밀 메모는 이 저장소(Book Service) 범위 밖(다른 MSA 컴포넌트 담당)으로 확인.

## 미완료 / 진행 중

`.harness/PLAN.md` 참조.
