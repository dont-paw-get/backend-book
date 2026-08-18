# ARCHITECTURE (현재 상태)

이 문서는 지금 시점의 실제 기술 스택·구조·컨벤션만 담는다. 결정 이유는 `DECISIONS.md`, 진행 상황은 `STATE.md`를 본다.

## 기술 스택

- Java 21, Spring Boot 4.1.0, Gradle Wrapper 9.5.1
- Spring MVC, Spring Data JPA
- 기준 패키지: `com.chc.dpgb`
- DB: PostgreSQL
- Lombok (compile/annotation processor)
- 실제 버전은 `build.gradle`과 Gradle Wrapper가 최종 기준

## 저장소 구조

루트 단일 Gradle 프로젝트다. `backend` 하위 모듈은 없다.

```text
src/main/java/com/chc/dpgb
└─ DpgbApplication.java

src/main/resources
└─ application.yaml

src/test/java/com/chc/dpgb
└─ DpgbApplicationTests.java
```

## 서비스 경계

이 저장소는 Book Service(Java, 이 프로젝트)이며, 독립된 Python RAG Service와 별도로 개발된다. 두 서비스는 각자 PostgreSQL을 소유하고 DB를 직접 공유하지 않는다.

## 테스트 구조

- `test`: 단위 테스트(Domain/Application unit, `@WebMvcTest`). DB 없음.
- `integrationTest`: PostgreSQL Testcontainers 기반 통합 테스트. 아직 Gradle에 구성되지 않았다 (`.harness/PLAN.md` 참조).
- 현재 유일한 테스트는 `DpgbApplicationTests`(`@SpringBootTest` 빈 smoke test).

## API 문서

- wire 계약: `docs/api/openapi.yaml`
- 사용 안내: `docs/api/README.md`
- 계약 결정: `docs/api/decisions/`

## Git

- 원격: `origin` = `https://github.com/dont-paw-get/backend-book.git`
- 브랜치: `main`(릴리스), `develop`(통합), `{티켓번호}-{설명}`(작업)
- 커밋 컨벤션: 저장소 루트 `README.md` 참조
