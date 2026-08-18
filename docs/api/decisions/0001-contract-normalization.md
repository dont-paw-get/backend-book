# ADR-0001: Book Service API 계약 정규화

- 상태: Accepted
- 일자: 2026-08-17

## 배경
초기 API 표는 endpoint, 예시 body, 성공/오류 응답을 제공하지만 GET body, 파일의 JSON 표현, path 변수의 body 중복, 식별자 타입과 204 body 등 HTTP 계약상 해석이 필요한 부분이 있다.

## 결정
1. 공식 계약은 OpenAPI 3.1 파일 `docs/api/openapi.yaml`로 관리한다.
2. OCR 이미지는 `multipart/form-data`의 `file` part로 받는다.
3. GET의 검색·필터·정렬·pagination은 query parameter로 받는다.
4. `{bookId}`는 path에서만 받고 request body에 반복하지 않는다.
5. `bookId`는 int64 JSON number로 통일한다.
6. DELETE 성공은 body 없는 204로 정의한다.
7. 시각은 offset을 포함한 ISO-8601 `date-time`으로 반환한다.
8. 기존 응답 키 `S3ImageUrl`은 소비자 호환을 위해 유지한다.
9. 모든 endpoint는 Bearer JWT 인증을 요구한다.
10. 제공된 status와 stable error code를 endpoint별 계약 테스트 대상으로 삼는다.

## 결과
- 프론트엔드와 백엔드가 동일한 기계 판독 계약을 사용할 수 있다.
- MockMvc 계약 테스트와 client/type 생성에 OpenAPI를 활용할 수 있다.
- 비표준 GET body 및 204 body 처리 차이에서 발생할 수 있는 호환 문제를 방지한다.
- `S3ImageUrl`은 JSON naming convention과 다르지만 현재 호환성을 우선한다.

