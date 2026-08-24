# backend-book
도서 메타데이터 등록(OCR/LLM) 및 독서 진도율(%) 트래킹 API 서버

## 문서
- [`AGENTS.md`](./AGENTS.md): 개발 하네스 워크플로우, DB·테스트·브랜치 정책
- [`.harness/`](./.harness/): 크로스 툴(Claude Code/Codex/Kiro) 작업 연속성 — 인수인계, 진행 상황, 아키텍처, 계획, 결정, 백로그
- [API 계약](docs/api/openapi.yaml): OpenAPI 3.1 wire 계약
- [API 문서 안내](docs/api/README.md): API 문서 탐색 진입점
- [`k8s/`](./k8s/), [`argocd/`](./argocd/), [`.github/workflows/build-push-ecr.yml`](./.github/workflows/build-push-ecr.yml): EKS 배포(GitOps) 구성 — 상세는 [`.harness/ARCHITECTURE.md`](./.harness/ARCHITECTURE.md)의 "배포 (EKS)" 절 참조

## 📝 커밋 메시지 컨벤션 (Commit Convention)
우리 팀은 자동화 도구(Changelog 등)와의 호환성을 위해 **'타입(Type)'은 영어**를 유지하되, **'제목(Subject)'과 '본문(Body)'은 명확한 한국어**로 작성합니다.

### 1. 기본 구조
> <타입>[적용 범위(선택)]: <제목 (요약)>
> 
> [본문(선택)]
> 
> [꼬리말(선택)]

### 2. 타입 (Type) 정의
* **`feat`** : 새로운 기능 추가
* **`fix`** : 버그 수정
* **`docs`** : 문서 수정 (README, API 명세서 등)
* **`style`** : 코드 포맷팅, 세미콜론 누락 등 (코드 로직 변경 없음)
* **`refactor`** : 코드 리팩토링 (기능 변경 없이 코드 구조만 개선)
* **`test`** : 테스트 코드, 리팩토링 테스트 코드 추가
* **`chore`** : 빌드 업무 수정, 패키지 매니저 설정 등

### 3. 제목 (Subject) 작성 규칙
* **명사형 어미로 끝내기:** `~함`, `~해라` 대신 `~추가`, `~수정`, `~구현` 등으로 간결하게 작성합니다.
* **글자 수 제한:** 50자 이내로 작성하여 한눈에 들어오게 합니다.
* **마침표 금지:** 제목 끝에 마침표(`.`)를 찍지 않습니다.

### 💡 팀 프로젝트 맞춤 팁 (Scope 활용)
MSA 아키텍처 특성상 도메인이 나뉘어 있으므로, 괄호 안에 작업한 세부 도메인을 명시해 주면 추후 히스토리 추적이 훨씬 쉬워집니다.

**✅ 좋은 커밋 메시지 예시**
> `feat(curation): 시간대 기반 룰 매칭 API 구현`
> `fix(db): 무드 필터링 시 GIN 인덱스 누락 문제 해결`
> `docs: 동물 사서 캐릭터 기반의 유저 플로우 기획서 업데이트`
