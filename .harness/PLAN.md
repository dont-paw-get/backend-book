# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

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
