---
inclusion: fileMatch
fileMatchPattern:
  - "src/**/*.java"
  - "src/main/resources/**/*.yaml"
  - "src/main/resources/**/*.yml"
  - "build.gradle"
  - "settings.gradle"
---
# Java / Spring Boot 구현 관례

이 문서는 **기술 스택과 Java·Spring·JPA 구현 관례**만 소유한다. 패키지 경계는 `architecture.md`, 업무 불변식은 `domain.md`, 테스트 방식은 `tdd.md`를 따른다.

## 기준 스택
- Java 21
- Spring Boot 4.1
- Gradle Wrapper 9.5.1
- Spring MVC, Spring Data JPA
- H2는 현재 로컬 개발용
- 기준 패키지 `com.chc.dpgb`

실제 버전은 `build.gradle`과 Gradle Wrapper가 최종 기준이다.

## Java
- API DTO에는 불변 `record`를 우선 검토한다.
- 클래스와 메서드는 한 책임을 가지며 의미 없는 `Util`, `Manager`, `Helper`에 로직을 모으지 않는다.
- nullable 상태와 단위를 타입·이름으로 드러낸다.
- entity 필드와 메서드 인자에 `Optional`을 사용하지 않는다.
- JPA entity에 Lombok `@Data`, 공개 setter, 무제한 builder를 사용하지 않는다.

## Spring
- 생성자 주입을 사용한다.
- Controller에서 Repository를 직접 호출하지 않는다.
- 형식 검증은 요청 경계, 업무 규칙은 domain/application에 둔다.
- DB 트랜잭션은 짧은 application 유스케이스에 둔다.
- 외부 호출을 DB 트랜잭션에 포함하지 않는다.
- 외부 설정은 `@ConfigurationProperties`로 묶고 시작 시 검증한다.
- 비밀값 기본값을 코드나 설정에 넣지 않는다.

## JPA
- entity는 보호된 기본 생성자와 의도가 드러나는 상태 변경 메서드를 제공한다.
- entity를 API 응답으로 직접 직렬화하지 않는다.
- 연관관계는 기본 LAZY로 설계한다.
- 목록 쿼리는 N+1, pagination, 정렬 허용 목록과 인덱스를 함께 검토한다.
- 애플리케이션 선검사만 믿지 않고 가능한 업무 불변식을 DB 제약으로 보호한다.
- 운영 환경에서는 명시적 migration 없이 `ddl-auto=update/create`를 사용하지 않는다.

## 경계 구현
- domain/application이 MVC, JPA entity, AWS·OCR·Bedrock SDK 타입에 의존하지 않게 한다.
- API JSON 이름과 Java 이름이 다르면 명시적으로 매핑한다.
- 전역 예외 처리기는 도메인·애플리케이션 오류를 OpenAPI 계약으로 변환한다.
- 인터페이스는 외부 경계나 대체 구현이 실제로 필요할 때 만든다.
