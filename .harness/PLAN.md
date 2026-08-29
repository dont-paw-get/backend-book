# PLAN (미완료 계획)

완료된 항목은 여기 체크만 남기지 않고 `STATE.md`로 옮긴 뒤 이 문서에서 제거한다.

## prod 배포 CrashLoopBackOff 해결 — 배포·검증 잔여 단계 (CLIAR-112)

코드 변경(Dockerfile 멀티아키 + CI buildx 전환)과 문서 동기화는 완료해 커밋했다 — `STATE.md` 참조.
사용자가 "커밋까지"로 범위를 정했으므로 아래는 **사용자가 직접 수행하거나 별도로 요청할 때** 진행한다.

### 배포

- [ ] `CLIAR-112-Book-Server-EKS-prod-배포` 브랜치 push
- [ ] `develop`으로 PR 생성·병합 → dev CI가 멀티아키 이미지를 빌드하는지 확인
- [ ] `develop` → `main` 병합 → prod CI 실행
  - prod ECR은 IMMUTABLE이라 기존 SHA 재push는 실패한다. 병합 커밋의 **새 SHA**로 push되어야 한다.
  - `Dockerfile`/워크플로우 변경은 `paths-ignore`(k8s/argocd/docs/*.md)에 걸리지 않으므로 정상 트리거된다.
- [ ] CI가 `k8s/overlays/prod/kustomization.yaml`의 `newTag`를 새 SHA로 갱신 커밋하는지 확인
- [ ] ArgoCD가 prod Application을 동기화하는지 확인

### 검증

- [ ] ECR 이미지가 실제로 멀티아키인지 확인 (아키텍처 2개 + attestation 없음)
      `aws ecr batch-get-image --repository-name dpyb-prod/dpyb-book --image-ids imageTag=<새SHA> --query 'images[0].imageManifest'`
      → `manifest.list.v2`(또는 OCI `index`)에 `amd64`/`arm64` 두 항목이 보여야 한다.
- [ ] `kubectl --context dpyb-prod -n dpyb-book get pods` 가 `2/2 Running`
- [ ] 잔여 ReplicaSet(`backend-book-76c75b8c48`, `backend-book-d5798fd87`) 정리 확인
- [ ] ArgoCD에서 Deployment health가 Degraded → Healthy 전환
- [ ] `/health` 200 응답 확인

### 주의: 다음 단계 장애가 처음 드러날 수 있음

prod에서 JVM이 **한 번도 기동한 적이 없다**. 아키텍처를 고치면 그 뒤 단계의 문제가 처음 보일 수 있다.
prod Secret(`backend-book-secret`)에 `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`/`ALADIN_API_TTB_KEY` 4개 키가
모두 존재하는 것은 확인했으나, **RDS 실제 접속·Flyway 마이그레이션·Cognito 검증은 미확인**이다.

- [ ] 파드가 뜬 직후 `kubectl --context dpyb-prod -n dpyb-book logs deploy/backend-book` 로
      Flyway 마이그레이션과 Spring 기동 로그를 확인한다
- [ ] 여전히 죽으면 이번엔 스택트레이스가 남으므로 그것을 근거로 다음 원인을 판단한다

### 긴급 롤백 레버

멀티아키 CI가 어떤 이유로든 막히고 prod를 즉시 살려야 하면, `k8s/cluster/nodepool-book.yaml`의
`requirements`에 아래를 추가하고 `dpyb-prod` 컨텍스트에 `kubectl apply` 한다(임시 조치 — Graviton
비용 이점을 포기하고 dev의 잠재 결함도 그대로 남으므로, 멀티아키 전환 후 반드시 되돌린다).

```yaml
        - key: kubernetes.io/arch
          operator: In
          values: ["amd64"]
```
