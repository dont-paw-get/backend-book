# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## DB 스키마 대개편 TODO (설계 방향 확정 — `.harness/DECISIONS.md` 2026-08-25 참조)

사용자가 제공한 확정 SQL과 이번 세션 결정으로 스키마 방향(PK `id` 통일, `member_id` UUID화, `shelf`/`library_book`/`scrap`/`librarian` soft delete, `genre`/`reading_status` 재도입, `librarian` 마스터→회원 소유 인스턴스 전환 + `librarian_type_info`/`librarian_level` 신설, `is_representative`로 ADR-0009 대체, `evolution_stage` 폐기)는 확정됐다. 결정 이유는 `DECISIONS.md`, 업무 규칙 반영은 구현 후 `DOMAIN.md`를 본다.

### 우선순위 1 — 내 결정 필요 (구현 착수 전에 확인)

- [ ] **API 엔드포인트 설계 확정** — 제안: `GET /api/v1/librarian-types`(타입 카탈로그, 기존 `getLibrarians` 대체) / `POST /api/v1/librarians`(사서 획득, `type`+`name` 필수, 타입별 1마리 제약 409) / `GET /api/v1/librarians`(내 보유 목록) / `PATCH /api/v1/librarians/{id}`(이름 변경) / `PATCH /api/v1/librarians/{id}/representative`(대표 지정) / `GET /api/v1/librarians/representative`(대표 조회). 이대로 진행할지, 경로·메서드를 다르게 할지.
- [ ] **사서 개명(이름 변경) 허용 여부** — 기본 제안은 위 `PATCH /api/v1/librarians/{id}`로 허용(다른 aggregate와 동일한 CRUD 관례). 등록 시 1회만 짓고 이후 불변으로 할지.
- [ ] **사서 삭제(방출) API 필요 여부** — 기본 제안은 만들지 않음(스펙에 없음). 필요하면 범위에 추가할지.
- [ ] **Flyway 마이그레이션 파일 분할 단위** — `librarian_type`/`librarian_type_info`/`librarian_level`/`librarian` 재설계를 한 파일로 묶을지, 테이블/aggregate별로 여러 `V7`~`V?`로 쪼갤지.
- [ ] **soft delete 부수효과 기본안 확인** — Shelf 삭제는 기존처럼 책을 기본 책장으로 이동시킨 뒤 그 Shelf 행만 soft delete, LibraryBook soft delete 시 소속 Scrap 전체를 애플리케이션이 벌크로 soft delete(기존 `ON DELETE CASCADE` 대체). 이대로 진행해도 되는지.

### 우선순위 2 — 구현 순서 (우선순위 1이 정리된 뒤 순차 진행)

- [ ] Flyway 마이그레이션(`V7`~) — `librarian_type` enum → `librarian_type_info` → `librarian_level`(+`level=1,required_experience=0` 최소 시드) → `librarian` 순으로 테이블 생성 순서 수정(사용자 제공 SQL의 순서 오류 반영), `shelf`/`library_book`/`scrap` PK 리네이밍·`member_id` UUID화·`deleted_at`·`genre`/`reading_status` 컬럼 추가. `V2`~`V6`는 `develop`에 이미 병합되어 직접 수정하지 않는다.
- [ ] 엔티티 변경 — `Shelf`/`LibraryBook`/`Scrap`/`Librarian`에 `deletedAt` 추가, `memberId` 타입 `String`→`UUID`(`MemberIdResolver.resolve` 포함). `LibraryBook`에 `genre`/`readingStatus` 필드 재도입. `Librarian`을 회원 소유 인스턴스 엔티티로 전면 재작성(`memberId`/`type`/`name`/`level`/`experience`/`isRepresentative`). 신규 `LibrarianLevel`/`LibrarianTypeInfo` 엔티티 추가.
- [ ] 리포지토리/서비스/컨트롤러 — 전 조회 쿼리에 `deleted_at IS NULL` 필터 일괄 적용, 우선순위 1에서 확정된 API 표면대로 `librarian` 패키지 재작성, `ShelfService`/`LibraryBookService`/`ScrapService` 삭제 로직을 soft delete 기준으로 수정.
- [ ] `docs/api/openapi.yaml` — `genre`/`readingStatus` 필드·필터 재도입, `Librarian` 관련 스키마/엔드포인트 전면 교체, `Scrap`에 `scrapImageUrl` 필수 필드 추가.
- [ ] `.harness/DOMAIN.md` — `LibraryBook`에 genre/readingStatus 규칙 복원, `Librarian` 절을 "회원 소유 사서 aggregate(레벨/경험치/대표 여부)"로 전면 재작성, soft delete를 전 aggregate 공통 규칙으로 명시.
- [ ] `.harness/ARCHITECTURE.md` — 마이그레이션 목록·librarian 패키지 구조·`MemberIdResolver` 반환 타입 갱신.
- [ ] 신규 ADR — ADR-0003(장르 제거)·ADR-0005(ReadingStatus 제거)·ADR-0009(대표 사서 Member 이관)를 반전하는 결정과 사유를 새 ADR 번호로 기록, 각 원본 ADR에 "이 결정은 ADR-00xx로 반전됨" 각주 추가.
- [ ] `.harness/BACKLOG.md` — `librarian_level`의 나머지 레벨 정책 값 확정 필요 항목 추가.
- [ ] 테스트 — 마이그레이션 순서 변경에 따른 통합 테스트, soft delete 조회 필터 회귀 테스트, `Librarian` 전면 재작성에 따른 도메인/서비스/컨트롤러 테스트 재작성.

## EKS 배포 CI/CD 구축 (deploy-dev 브랜치)

`backend-record`의 GitOps 패턴(GitHub Actions → ECR push → Kustomize 이미지 태그 갱신 커밋 → ArgoCD auto-sync)을 그대로 이식한다. 사용자 확인 사항: 브랜치는 `deploy-dev` 그대로 사용(컨벤션 예외), AWS 자격증명 GitHub Secrets는 이미 설정됨, 헬스체크는 Actuator 대신 인증 없는 커스텀 `/health` 컨트롤러, Dockerfile은 변경하지 않음.

전제: ECR `594532711953.dkr.ecr.ap-northeast-2.amazonaws.com/dpyb-dev/dpyb-book` 생성 완료, EKS 클러스터 `dpyb-dev` 존재, ALB Ingress Controller/IngressClass `alb`는 `backend-auth` 레포에서 클러스터 전역으로 이미 구성됨(backend-record와 공유).

### 체크리스트

- [ ] `/health` 헬스체크 컨트롤러 추가: 인증 없는 `GET /health` → `200 {"status":"UP"}`. `SecurityConfig`의 `permitAll()` 목록에 `/health` 추가(기존 `/docs/**`, `/webjars/**`, `/openapi.yaml`과 동일 패턴). 단위 테스트 추가.
- [ ] `k8s/base/`: `deployment.yaml`(non-root 불필요, 기존 Dockerfile 그대로 root 실행 — `securityContext` 최소화), `service.yaml`(ClusterIP, port 80 → targetPort 8080), `ingress.yaml`(ALB, healthcheck-path `/health`), `configmap.yaml`(민감하지 않은 설정: `SPRING_PROFILES_ACTIVE=prod`), `kustomization.yaml`
- [ ] `k8s/overlays/dev/`: `kustomization.yaml`(namespace `dpyb-book-dev`, replicas 1, image `.../dpyb-dev/dpyb-book:develop-latest`, CI가 태그 갱신), `configmap-patch.yaml`(dev Cognito 등 dev 전용 값 — 실제 값은 사용자가 채워야 함, 아래 "구현 전 확인 필요" 참조)
- [ ] `k8s/overlays/prod/kustomization.yaml`: backend-record처럼 전체 주석 처리(prod 배포는 아직 사용 안 함)
- [ ] `k8s/secret.example.yaml`: 실제 값 없는 예시 구조만(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`ALADIN_API_TTB_KEY`) — 실제 Secret은 Git에 커밋하지 않고 사용자가 `kubectl create secret` 또는 SealedSecrets로 클러스터에 직접 생성
- [ ] `argocd/application-dev.yaml`: repoURL `https://github.com/dont-paw-get/backend-book.git`, targetRevision `develop`, path `k8s/overlays/dev`, namespace `dpyb-book-dev`, automated prune+selfHeal
- [ ] `argocd/application-prod.yaml`: backend-record처럼 전체 주석 처리
- [ ] `.github/workflows/build-push-ecr.yml`: `develop` push 시 ECR(`dpyb-dev/dpyb-book`) 빌드/푸시(SHA 태그 + `develop-latest`), `k8s/overlays/dev/kustomization.yaml`의 `newTag`를 SHA로 갱신하는 커밋을 같은 브랜치에 push (`paths-ignore`로 무한루프 방지)
- [ ] `.harness/ARCHITECTURE.md`에 배포 구조(k8s/ArgoCD/CI 파이프라인 개요) 반영
- [ ] 루트 `README.md`에 배포 관련 문서 링크 추가 여부 검토(CLAUDE.md 산출물 동기화 규칙)

### 구현 전 확인 필요 (사용자가 값을 채워야 실제 배포 가능)

- dev 환경 PostgreSQL 접속 정보(`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) — RDS 등 dev DB 준비 여부
- dev 환경 Cognito `AUTH_ISSUER_URI`/`AUTH_APP_CLIENT_ID` (prod와 별도 User Pool인지, 아니면 공용인지)
- `ALADIN_API_TTB_KEY` 운영/dev 공용 여부
- ArgoCD가 이미 클러스터에 떠 있고 `backend-auth` 레포의 IngressClass `alb` 리소스가 이미 적용돼 있는지 (backend-record가 이미 배포됐다면 이미 준비됐을 가능성 높음 — 확인만 요청)

이 파일들을 만들고 나면 실제 배포(ArgoCD Application 적용, Secret 생성)는 위 값들이 채워진 뒤 사용자가 진행한다.
