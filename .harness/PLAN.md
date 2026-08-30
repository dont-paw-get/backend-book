# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## B. dev DB 분리 — book 완료, auth·record 전환 대기

dev Aurora에 서비스별 데이터베이스를 만들고 **데이터를 복사**했다(옮긴 것이 아니라 복사 —
각 팀이 준비되면 전환하면 되고 그 전까지 아무도 깨지지 않는다). book은 전환·검증까지 끝났다.

| DB | 상태 |
| --- | --- |
| `dpyb_book` | ✅ 복사 완료, book 서비스 전환·기동 검증 완료 |
| `dpyb_auth` | 복사 완료 — **auth 팀이 전환하면 됨** |
| `dpyb_record` | 복사 완료(테이블은 `alembic_version_record` 하나, 0행) — **record 팀이 전환하면 됨** |

### auth·record 팀이 할 일

각 저장소가 아니라 **dev 클러스터의 Secret만** 바꾸면 된다(Secret은 Git에 없어 ArgoCD가 되돌리지 않는다).
`DATABASE_URL`의 데이터베이스 이름만 `dpyb` → `dpyb_auth` / `dpyb_record` 로 바꾸고 파드를 재생성한다.

- [ ] auth: `dpyb-auth-dev/backend-auth-secret` 의 `DATABASE_URL` 변경 후 파드 재생성·검증
- [ ] record: `dpyb-record-dev/backend-record-secret` 의 `DATABASE_URL` 변경 후 파드 재생성·검증

### 전원 전환 후

- [ ] 일정 기간(롤백 대비) 유지한 뒤 기존 `dpyb` 데이터베이스 정리
      — 세 서비스가 모두 새 DB에서 정상 동작하는 것을 확인한 다음에만 진행
- [ ] auth·record 팀 안내용 런북(전환 절차·검증·롤백)은 Artifact로 작성해 공유했다
- [ ] 같은 김에 `test` 데이터베이스 삭제(`BACKLOG.md`)

### 참고: 분리가 깨끗했던 이유

서비스 경계를 넘는 외래키가 **하나도 없었다**.

```
book : librarian→librarian_level, librarian→librarian_type_info,
       library_book→shelf, scrap→library_book
auth : member_agreement→member, member_agreement→terms, member_librarian→member
```

커스텀 enum 타입은 `-t` 덤프에 포함되지 않아 대상 DB에 먼저 만들어야 했다
(book: `book_reading_status`/`genre_type`/`librarian_type`,
auth: `member_agreement_action`/`member_gender`/`member_status`).
`pg_dump`는 서버(17.7)와 같은 메이저 버전이어야 해서 `postgres:17-alpine` 클라이언트를 썼다.
