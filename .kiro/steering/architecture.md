---
inclusion: always
---
# Book Service 아키텍처

이 문서는 **서비스 책임, 내부 경계, 목표 프로젝트 구조, 서비스 간 통신**만 소유한다. 제품 흐름은 `product.md`, 도메인 규칙은 `domain.md`, 기술 관례는 `java-spring.md`, HTTP 계약은 `docs/api/openapi.yaml`을 따른다.

## 서비스 경계
이 저장소는 Virtual Shelf MSA에서 독립 배포되는 **Book Service**다. 하나의 Spring Boot 애플리케이션과 전용 데이터 저장소를 소유한다.

담당 기능:
- 표지 이미지 저장과 OCR 제목·저자 추출
- 외부 도서 메타데이터 검색
- 도서 무드 AI 분석
- 로그인 사용자의 개인 서재 CRUD·필터·정렬
- 현재/전체 페이지와 독서 상태 관리

비담당 기능:
- 회원가입, 로그인, 토큰 발급
- 기분 기반 최종 추천
- 문장 OCR, 감상·비밀 메모
- API Gateway/BFF

다른 서비스의 DB를 직접 읽거나 수정하지 않는다. 서비스 간 공유는 공개 API 또는 합의된 이벤트를 사용한다.

## 목표 프로젝트 구조
현재 저장소는 애플리케이션 진입점만 있는 초기 골격이다. 다음 구조는 구현할 목표이며, 실제 코드와 함께 단계적으로 갱신한다.

```text
src/main/java/com/chc/dpgb
├─ DpgbApplication.java
├─ bookdiscovery
│  ├─ api
│  ├─ application
│  │  ├─ port/in
│  │  ├─ port/out
│  │  └─ service
│  ├─ domain
│  └─ infrastructure
│     ├─ storage
│     ├─ ocr
│     ├─ catalog
│     └─ bedrock
├─ library
│  ├─ api
│  ├─ application
│  │  ├─ port/in
│  │  ├─ port/out
│  │  └─ service
│  ├─ domain
│  └─ infrastructure/persistence
├─ security
└─ common
   ├─ error
   └─ config

src/test/java/com/chc/dpgb
├─ bookdiscovery
├─ library
├─ security
└─ architecture
```

`bookdiscovery`와 `library`는 서로의 Controller를 호출하지 않는다. `common`에는 두 기능에서 실제로 안정된 오류·설정만 둔다.

## 데이터와 기능 소유권
- `bookdiscovery`: OCR·외부 검색·AI 분석 결과를 생성하며 사용자 확정 전 후보를 다룬다.
- `library`: 사용자별 `LibraryBook` aggregate와 진도를 영속화한다.
- `security`: 검증된 인증 principal을 서비스의 사용자 식별자로 변환한다.
- `integration infrastructure`: 공급자 DTO를 내부 모델로 변환하는 anti-corruption layer다.

구체적인 필드와 불변식은 `domain.md`가 소유한다.

## 외부 연동 포트
- `ImageStoragePort`: 이미지 저장·삭제
- `BookCoverOcrPort`: 표지에서 제목·저자·신뢰도 추출
- `BookCatalogPort`: 외부 도서 후보 조회
- `BookAiAnalysisPort`: 장르·무드·보완 결과 생성

외부 SDK와 공급자 DTO는 infrastructure 밖으로 노출하지 않는다. 외부 호출은 DB 트랜잭션과 분리한다.

## MSA 통신 원칙
- 인증 서비스가 발급한 신뢰 가능한 토큰의 검증 결과로 사용자 식별자를 얻는다.
- 동기 호출에는 connect/read timeout과 제한된 재시도를 둔다. 비멱등 요청은 자동 재시도하지 않는다.
- 이벤트가 필요해질 때만 transactional outbox와 버전 계약을 도입한다.
- correlation/trace ID를 전파하고 서비스는 stateless하게 유지한다.
- 이미지는 로컬 디스크에 영구 저장하지 않는다.

## 미결정 아키텍처
- 운영 DB와 migration 도구
- JWT 검증 경계와 issuer/audience
- OCR·도서 API·Bedrock 공급자와 client
- S3 bucket, 객체 수명과 URL 제공 방식
- 동기 처리 한계를 넘을 때의 비동기 job/event 구조

결정 시 코드와 이 문서, 설정, 배포·운영 산출물을 함께 갱신한다.
