# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `C:\reviewer\workspace\과제\SampleOrderSystem\CLAUDE.md`
- `C:\reviewer\workspace\과제\SampleOrderSystem\docs\PRD.md` (v1.1.0)
- `C:\reviewer\workspace\과제\SampleOrderSystem\docs\PLAN.md` (v1.1.0)
- `C:\reviewer\workspace\과제\SampleOrderSystem\docs\design\phase3.md` (v1.0.0)

**결과**: 문제 4건 발견 (CRITICAL: 0, WARNING: 3, INFO: 1)

---

## 발견된 문제

### [WARNING] OrderView.printOrderList() 메서드 시그니처 불일치

- **위치**: `docs/PLAN.md` Phase 3 설계 상세 > OrderView 섹션 vs `docs/design/phase3.md` Section 4-1
- **설명**: PLAN.md에는 `void printOrderList(List<Order> orders)` (파라미터 1개)로 정의되어 있으나, phase3.md에서는 `void printOrderList(List<Order> orders, Map<String, String> sampleNames)` (파라미터 2개)로 확장 정의되었다. 구현 시 어느 시그니처를 따를지 혼란이 발생할 수 있다.
- **권장 조치**: PLAN.md의 OrderView 메서드 시그니처를 phase3.md 기준으로 업데이트한다. (`Map<String, String> sampleNames` 파라미터 추가)

---

### [WARNING] ProductionLineController.enqueueItem() 메서드 — PLAN.md에 누락

- **위치**: `docs/PLAN.md` Phase 3 > ProductionLineController 메서드 목록 vs `docs/design/phase3.md` Section 6
- **설명**: PLAN.md의 ProductionLineController 메서드 목록에는 `showQueue()`, `completeProduction()`, `createQueueItem()` 3개만 정의되어 있다. 그러나 phase3.md Section 6에서 `OrderController`가 `queueRepo`에 직접 접근하지 않도록 `public void enqueueItem(ProductionQueueItem item)` 메서드를 추가 정의하였다. PLAN.md에 이 메서드가 반영되지 않았다.
- **권장 조치**: PLAN.md의 ProductionLineController 메서드 목록에 `void enqueueItem(ProductionQueueItem item)` 을 추가한다.

---

### [WARNING] PLAN.md 내부 모순 — Router 메뉴 수 불일치 (5개 vs 6개)

- **위치**: `docs/PLAN.md` Section 6.2 ConsoleMVC 이식 표 Router 행 vs `docs/PLAN.md` Phase 5 Router 설계 및 `docs/PRD.md` Section 3.1
- **설명**: PLAN.md Section 6.2 이식 표의 Router 행에는 "단일 Controller → 5개 Controller 주입, 5개 메뉴로 확장"이라고 기재되어 있다. 그러나 PRD.md Section 3.1, PLAN.md Phase 5 `route()` 메서드 설계, CLAUDE.md 패키지 구조 주석 모두 6개 메뉴([1]~[6])임을 명시하고 있다. PLAN.md 내부에서 서로 상충하는 수치가 존재한다.
- **권장 조치**: PLAN.md Section 6.2 ConsoleMVC 이식 표의 Router 행을 "5개 Controller 주입, 6개 메뉴로 확장"으로 수정한다.

---

### [INFO] CLAUDE.md 코딩 컨벤션 예시의 ID 포맷 표기 불일치

- **위치**: `CLAUDE.md` 코딩 컨벤션 섹션 "ID 자동 생성 규칙" 항목 vs `docs/PRD.md` Section 8.1 ID 형식
- **설명**: CLAUDE.md 코딩 컨벤션의 ID 자동 생성 규칙 예시는 `"S" + String.format("%03d", n)` (하이픈 없음, 예: `S001`)이다. 그러나 CLAUDE.md 핵심 설계 결정 사항, PRD.md Section 8.1, PLAN.md, phase3.md 모두 `S-001` (하이픈 포함) 포맷을 사용한다. 코드 예시와 ID 형식 명세가 불일치한다.
- **권장 조치**: CLAUDE.md 코딩 컨벤션의 ID 자동 생성 예시를 `"S-" + String.format("%03d", n)` 으로 수정하여 PRD Section 8.1 ID 형식과 일치시킨다.

---

## 통과 항목

- [A] 교차 참조: 이상 없음 (phase3.md의 PLAN.md/PRD.md 링크 경로 정확, Phase 0/4/5 문서 미작성은 진행 중인 프로젝트 특성상 정상)
- [B] 기술 스택: 이상 없음 (phase3.md에서 신규 외부 의존성 추가 없음, Java 표준 라이브러리만 사용)
- [C] 설계 제약 반영: 이상 없음 (MVC 레이어 규칙 준수, 생성자 주입 준수, `System.out` 직접 출력 금지 준수, 수율 계산 위치 규칙 준수)
- [D] 완료 기준: 이상 없음 (phase3.md Section 8에 FR별 체크리스트 형식으로 명시)
- [E] 내부 모순: 문제 3건 (WARNING 항목으로 보고됨)
