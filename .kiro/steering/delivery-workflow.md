---
inclusion: manual
---
# Book Service 개발 순서

이 문서는 **기능 구현의 단계와 의존 순서**만 소유한다. 각 단계의 계약은 OpenAPI, 업무 규칙은 `domain.md`, 개발 방식은 `tdd.md`를 따른다.

## Phase 0. 기반
- access token의 서명·issuer·audience·만료 검증과 principal 변환
- 사용자 소유 데이터 격리
- 공통 오류 변환과 테스트 fixture
- outbound port의 deterministic fake

## Phase 1. Library domain
- `LibraryBook` 생성과 값 객체
- 페이지·독서 상태 전이
- 사용자별 중복 판정

## Phase 2. Library use case
OpenAPI의 Library와 Reading Progress operation을 작은 vertical slice로 하나씩 구현한다.

권장 순서:
1. 등록
2. 상세 조회
3. 목록·필터·정렬
4. 정보 수정
5. 진도 수정
6. 삭제

## Phase 3. Book Discovery use case
OpenAPI의 Book Discovery operation을 실제 공급자 없이 port fake로 구현한다.

권장 순서:
1. 표지 OCR
2. 외부 도서 검색
3. AI 도서 분석

## Phase 4. Outbound adapter
- 이미지 저장소
- OCR 공급자
- 외부 도서 API
- Bedrock

각 adapter는 독립된 경계 테스트로 보호한다.

## Phase 5. 운영 강화
- JWT 검증 정책
- timeout, retry와 connection 관리
- 관측성과 correlation ID
- DB migration과 동시성
- 이미지 수명·삭제
- 필요 시 event/outbox

각 단계가 끝나면 `spring-boot-verification` skill로 검증하고 `artifact-synchronization.md`에 따라 관련 산출물을 갱신한다.
