---
inclusion: fileMatch
fileMatchPattern:
  - "src/**/*.java"
  - "docs/api/openapi.yaml"
  - "docs/api/**/*.md"
---
# Book Service API 참조

이 steering은 API 내용을 복제하지 않고 Kiro가 공식 계약을 읽도록 연결한다.

- 공식 wire 계약: #[[file:../../docs/api/openapi.yaml]]
- 사용 안내: #[[file:../../docs/api/README.md]]
- 계약 정규화 결정: #[[file:../../docs/api/decisions/0001-contract-normalization.md]]

필드, parameter, status와 error code는 OpenAPI를 기준으로 구현한다. 계약 변경은 `artifact-synchronization.md`, 개발 방식은 `tdd.md`를 따른다.
