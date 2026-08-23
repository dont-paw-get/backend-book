# ADR-0009: 대표 사서 선택(`getMyLibrarian`/`selectMyLibrarian`)을 Member 서비스로 이관

- 상태: Accepted
- 일자: 2026-08-22

## 배경

Member 서비스 쪽 ERD 초안을 검토하던 중 `member` 테이블에 `representative_librarian_id` 컬럼이 있는 것을 확인했다. Book Service는 CLIAR-46에서 이미 같은 사실(회원이 선택한 대표 동물 사서)을 `member_librarian_selection` 테이블로 구현해뒀다 — 같은 사실이 두 서비스에 중복 저장되는 구조였다.

"대표 사서 선택"은 서재/책장/스크랩 같은 도서 관리 도메인과 무관하고, 회원 프로필에 딸린 개인화 설정에 가깝다. 반면 동물 사서의 마스터 카탈로그(이름/타입/이미지)는 Book Service가 계속 관리해야 할 이유가 있다(향후 책과 연계된 콘텐츠 확장 등). 그래서 "선택"과 "카탈로그"를 분리해, 선택은 Member 서비스가 소유하고 카탈로그는 Book Service가 계속 소유하기로 했다.

## 결정

1. `getMyLibrarian`(`GET /api/v1/members/me/librarian`), `selectMyLibrarian`(`PUT /api/v1/members/me/librarian`)을 Book Service 계약에서 제거한다. 두 endpoint는 같은 데이터(`member_librarian_selection`)를 다루므로 함께 제거한다 — 선택 API만 없애면 조회 API가 참조할 데이터가 사라진다.
2. `getLibrarians`(`GET /api/v1/librarians`, 사서 마스터 카탈로그 조회)는 Book Service에 그대로 남는다.
3. Book Service의 `member_librarian_selection` 테이블, `MemberLibrarianSelection` aggregate, 관련 포트/어댑터/DTO/예외(`LibrarianNotSelectedException`, `LibrarianNotFoundException`)를 전부 제거한다.

## 결과

- `docs/api/openapi.yaml`(v0.7.0)에서 `getMyLibrarian`/`selectMyLibrarian` endpoint, `MyLibrarianResponse`/`SelectLibrarianRequest` 스키마, `LibrarianNotSelected`/`LibrarianNotFound` 응답을 제거했다. `LibrarianType`/`LibrarianSummary`/`LibrarianListResponse`는 `getLibrarians`가 계속 사용하므로 유지한다.
- `src/main/resources/db/migration/V6__drop_member_librarian_selection.sql`로 `member_librarian_selection` 테이블을 DROP한다 — `V5`는 이미 `develop`에 병합돼 직접 수정하지 않는다. `librarian`(마스터) 테이블과 시드 데이터는 그대로 유지한다.
- `com.chc.dpgb.librarian` 패키지에서 `MemberLibrarianSelection`(domain), `MemberLibrarianSelectionRepository`+JPA 어댑터(application/infrastructure), `LibrarianService.getMyLibrarian`/`selectMyLibrarian`, `LibrarianController`의 해당 두 메서드, `MyLibrarianResponse`/`SelectLibrarianRequest`(web/dto)를 제거했다. `Librarian`(마스터)/`LibrarianRepository`/`LibrarianService.getLibrarians`/`LibrarianController.getLibrarians`는 그대로 유지한다.
- `getLibrarians`가 `librarianId` 유효성 검사의 유일한 참조 지점이 아니게 됐다 — Member 서비스가 `selectMyLibrarian`을 구현할 때 `librarianId` 유효성을 어떻게 검증할지(자체 카탈로그 사본을 두는지, Book Service의 `GET /api/v1/librarians`를 호출하는지)는 이 ADR의 범위 밖이며 Member 서비스 쪽에서 별도로 결정한다.
- 회원 탈퇴 시 Member 서비스의 `email`/`nickname` 익명화 정책, 그리고 탈퇴한 회원의 Book Service 데이터(서재/책장/스크랩) 처리는 이 ADR과 별개로 논의됐다 — 후자는 이벤트/알림 인프라가 아직 없어 지금 단계에서는 손대지 않기로 하고 `.harness/BACKLOG.md`로 이연했다.
