---
name: spring-boot-verification
description: Book Service 변경 후 테스트, 빌드, OpenAPI와 산출물 참조를 검증하고 결과를 보고할 때 사용한다.
---
# Spring Boot 검증

## 검증 선택
- 코드 변경: 가장 가까운 targeted test 후 `.\gradlew.bat test`
- 의존성·설정·패키지 구조 변경: `.\gradlew.bat build` 추가
- API 변경: OpenAPI YAML 파싱, 내부 `$ref`, operation/response 계약 확인
- 문서·하네스 변경: front matter, JSON, 파일 참조와 오래된 경로 확인
- 외부 adapter 변경: test double 기반 경계 테스트; 실제 sandbox는 별도 profile

## 실패 처리
첫 근본 원인과 관련된 변경만 수정하고 같은 검증을 다시 실행한다. 실패를 재실행으로 숨기지 않는다.

## 보고
다음을 한국어로 구분해 기록한다.
- 실행한 명령과 통과 결과
- 실패 후 수정한 내용
- 확인한 계약·참조
- 자격 증명이나 환경 부재로 검증하지 못한 외부 연동
