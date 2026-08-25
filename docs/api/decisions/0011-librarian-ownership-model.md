# ADR-0011: 사서(Librarian) 소유 모델 전면 개편 — 대표 사서 선택을 Book Service로 재통합 (ADR-0009 대체)

- 상태: Accepted
- 일자: 2026-08-25

## 배경

사용자가 제공한 확정 DB 스키마는 `librarian`을 기존 마스터 카탈로그(`librarian_id`/`name`/`type`/`image_url`/`evolution_stage`, Flyway 시드로만 채워짐)에서 **회원이 실제로 획득해 보유하는 인스턴스**(`member_id`/`type`/`name`/`level`/`experience`/`is_representative`)로 재정의한다. 대표 사서 여부(`is_representative`)가 다시 Book Service의 `librarian` 테이블에 포함되면서, ADR-0009("대표 사서 선택은 Member 서비스가 소유")를 다시 반전시키는 결정이 필요해졌다.

이번 확정 스키마는 CLIAR-46(최초 구현) → ADR-0009(Member 서비스 이관) → 이번 ADR(Book Service 재통합)로 세 번째 방향 전환이다. 이번에는 단순 "선택"이 아니라 레벨·경험치를 가진 회원 소유 게임 요소로 재정의된 것이 이전 두 번과 다른 점이다 — 사서를 "선택"하는 게 아니라 "획득"하고 키우는 모델이므로, 그 소유 관계와 대표 여부를 같은 서비스가 함께 관리하는 편이 자연스럽다고 판단했다.

## 결정

1. `librarian`을 회원 소유 사서 인스턴스로 재정의한다. 회원은 타입(`RUSSIAN_BLUE`/`SHOEBILL`)별로 최대 1마리를 획득할 수 있고, 각 사서는 `level`/`experience`/`isRepresentative`를 갖는다. 레벨업·경험치 획득 로직 자체는 이번 범위 밖이다 — 컬럼과 정책 테이블(`librarian_level`)만 두고 값을 바꾸는 비즈니스 규칙은 만들지 않는다.
2. 사서 타입별 공통 정보(이미지)는 신규 마스터 테이블 `librarian_type_info`(`type`/`imageUrl`/`clickedImageUrl`)로 분리한다. 기존 마스터 카탈로그가 갖던 `evolutionStage` 개념은 새 스키마에 없어 폐기한다.
3. 사서 이름은 회원이 직접 짓는다 — 서버가 타입 마스터 이름을 복사해 채우지 않으며, 언제든 개명할 수 있다(다른 aggregate와 동일한 CRUD 관례).
4. 대표 사서 선택·조회를 Book Service가 다시 소유한다(ADR-0009 대체). 회원당 대표 사서는 최대 1마리이며, 다른 사서를 대표로 지정하면 기존 대표는 자동으로 해제된다.
5. 사서 방출(삭제)을 이번 범위에 포함한다 — 다른 aggregate와 동일하게 soft delete로 처리한다.
6. API 표면을 `getLibrarians`(마스터 카탈로그 조회) 단일 endpoint에서 7종으로 재편한다: `getLibrarianTypes`(타입 카탈로그), `acquireLibrarian`(획득), `getLibrarians`(내 보유 목록), `renameLibrarian`(개명), `selectRepresentative`(대표 지정), `getRepresentative`(대표 조회), `deleteLibrarian`(방출).

## 결과

- `docs/api/openapi.yaml`(v0.8.0)에서 `getLibrarians`(마스터 카탈로그) 단일 endpoint와 `LibrarianType`/`LibrarianSummary`/`LibrarianListResponse`(구 형태) 스키마를 제거하고, 위 7개 endpoint와 `LibrarianTypeSummary`/`LibrarianTypeListResponse`/`AcquireLibrarianRequest`/`AcquireLibrarianResponse`/`LibrarianSummary`(신형)/`LibrarianListResponse`(신형)/`RenameLibrarianRequest`/`RenameLibrarianResponse`/`RepresentativeLibrarianResponse` 스키마, 관련 오류 응답 5종(`InvalidLibrarianData`/`LibrarianAlreadyOwned`/`LibrarianAccessDenied`/`LibrarianNotFound`/`RepresentativeLibrarianNotSelected`)을 추가했다.
- `com.chc.dpgb.librarian` 패키지를 전면 재작성했다: `Librarian`(마스터 엔티티 → 회원 소유 인스턴스 엔티티), 신규 `LibrarianType`(enum)/`LibrarianTypeInfo`(마스터, Flyway 시드 전용) 엔티티, `LibrarianService`/`LibrarianController`/DTO 전체.
- `LibrarianLevel` 엔티티는 만들지 않았다 — 레벨업 로직이 범위 밖이라 앱 코드가 레벨 정책 값을 조회하지 않는다. DB 테이블(`librarian_level`)과 FK 제약은 존재하지만 JPA 매핑 없이 순수 인프라로만 남겨뒀다(YAGNI, 레벨업 API를 만들 때 추가).
- `V9__redesign_librarian.sql`로 기존 `librarian`(마스터) 테이블을 DROP하고 `librarian_type` enum → `librarian_type_info` → `librarian_level`(`level=1,required_experience=0` 최소 시드) → `librarian` 순으로 재생성했다 — `member_librarian_selection`은 이미 ADR-0009 당시 V6에서 제거되어 옛 마스터 카탈로그를 참조하는 다른 테이블이 없었으므로 안전하게 DROP할 수 있었다.
- ADR-0009는 이 결정으로 대체된다 — ADR-0009 문서 자체는 append-only 이력으로 남기고, 별도 각주로 이 ADR을 가리킨다.
- `.harness/DOMAIN.md`의 Librarian 절을 "동물 사서 카탈로그 조회"에서 "회원 소유 사서 aggregate(레벨/경험치/대표 여부/방출)"로 전면 재작성한다(문서 갱신은 별도 작업으로 처리).
