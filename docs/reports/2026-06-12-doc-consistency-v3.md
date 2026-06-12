# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `SampleOrderSystem/CLAUDE.md`
- `docs/PRD.md` (v1.1.0)
- `docs/PLAN.md` (v1.1.0)

**결과**: 문제 5건 발견 (CRITICAL: 2, WARNING: 3, INFO: 0)

---

## 발견된 문제

### [CRITICAL-1] Router 메뉴 번호와 MainView 출력 레이아웃 불일치

- **위치**: `docs/PLAN.md` — Phase 5 `Router.route()` 설계 / Phase 5 `MainView.printMainMenu()` 설계
- **설명**:
  `MainView.printMainMenu()`는 PDF 화면 명세(`PRD.md` Section 8.2) 기반으로 아래와 같이 출력한다.
  ```
  [1] 시료 관리          [2] 시료 주문
  [3] 주문 승인/거절     [4] 모니터링
  [5] 생산라인 조회      [0] 종료
  ```
  그러나 `Router.route()` 분기는 다음과 같이 정의된다.
  ```
  1 → sampleController
  2 → orderController        (주문 접수·승인·거절 통합)
  3 → monitoringController
  4 → releaseController
  5 → productionLineController
  ```
  결과적으로 화면에는 `[3] 주문 승인/거절`이 표시되지만 번호 3을 입력하면 `monitoringController`가 실행된다.
  마찬가지로 `[4] 모니터링` 입력 시 `releaseController`(출고 처리)가 실행되고, 출고 처리 메뉴 자체가 화면에서 사라진다.
- **권장 조치**: 두 설계 중 하나를 기준으로 통일한다.
  - 안(A) — Router를 PDF 화면 명세에 맞춰 재정의: `2→주문 접수(신규), 3→주문 승인/거절, 4→모니터링, 5→생산라인` + 출고 처리 메뉴 번호 추가(예: `6` 또는 서브메뉴 통합)
  - 안(B) — MainView 출력을 Router 분기에 맞춰 수정: `[2]주문(접수/승인/거절) [3]모니터링 [4]출고 [5]생산라인`으로 변경 후 PRD Section 8.2도 업데이트

---

### [CRITICAL-2] ID 형식 3방향 충돌 (CLAUDE.md vs PRD.md vs PLAN.md 혼재)

- **위치**:
  - `CLAUDE.md` — "핵심 설계 결정 사항 > ID 타입" 및 "코딩 컨벤션"
  - `docs/PRD.md` — Section 8.1 ID 형식 테이블
  - `docs/PLAN.md` — Phase 1 `ProductionQueueItem`, Phase 3 `createQueueItem`, Phase 5 `SampleFixture` / `OrderFixture`
- **설명**:

  | 문서/위치 | 시료 ID | 주문 ID | 큐 ID |
  |----------|---------|---------|------|
  | CLAUDE.md (설계 결정) | `S001` (하이픈 없음) | 미명시 | `Q001` |
  | CLAUDE.md (코딩 컨벤션) | `"S" + String.format("%03d", n)` | — | — |
  | PRD.md Section 8.1 | `S-NNN` (예: `S-001`) | `ORD-YYYYMMDD-NNNN` | `Q-NNN` (예: `Q-001`) |
  | PLAN.md Phase 1 Entity | — | — | `"Q001" 포맷` |
  | PLAN.md Phase 2 출력 예 | `S-001` (하이픈 있음) | — | — |
  | PLAN.md Phase 3 orderId 생성 | — | `ORD-20250416-0001` | — |
  | PLAN.md Phase 5 SampleFixture | `"S001"` (하이픈 없음) | — | — |
  | PLAN.md Phase 5 OrderFixture | — | `"O001"` (하이픈 없음) | — |

  시료 ID: CLAUDE.md·SampleFixture는 `S001`, PRD 8.1·PLAN Phase 2 출력 예는 `S-001`.
  큐 ID: PLAN Phase 1·3는 `Q001`, PRD 8.1은 `Q-001`.
  주문 ID: PLAN Phase 3·PRD 8.1은 `ORD-YYYYMMDD-NNNN`으로 일치하나, OrderFixture는 `O001`(전혀 다른 형식) 사용.

  테스트 픽스처(SampleFixture, OrderFixture)의 ID가 실제 시스템의 ID 형식과 달라 통합 테스트에서 ID 참조 무결성 검증이 무의미해질 수 있다.

- **권장 조치**: PRD.md Section 8.1의 ID 형식을 단일 기준으로 확정하고, CLAUDE.md 코딩 컨벤션, PLAN.md 전체(Entity 설계, Phase 2 출력 예, Phase 5 픽스처)를 동일 형식으로 일괄 수정한다. 픽스처 ID는 실제 운영 ID 형식(`S-001`, `Q-001`)과 동일한 포맷을 사용해야 통합 테스트의 신뢰도가 유지된다.

---

### [WARNING-1] PRD.md Section 3.1 메인 메뉴와 Section 8.2 메인 화면 내부 모순

- **위치**: `docs/PRD.md` — Section 3.1 vs Section 8.2
- **설명**:
  - Section 3.1: 메뉴 항목 5개 — `1.시료 관리 / 2.주문(접수/승인/거절) / 3.모니터링 / 4.출고 처리 / 5.생산 라인`
  - Section 8.2: `[1]시료 관리 / [2]시료 주문 / [3]주문 승인/거절 / [4]모니터링 / [5]생산라인 조회` — 주문이 `접수`와 `승인/거절`로 분리, `출고 처리` 메뉴가 목록에서 누락됨.
  Section 8.2 추가 시 Section 3.1을 업데이트하지 않아 PRD 내부에서 메뉴 구성이 상충한다.
- **권장 조치**: Section 8.2 확정 메뉴 구성을 기준으로 Section 3.1을 동기화한다. 출고 처리(`FR-REL`) 메뉴 번호도 Section 8.2에 명시적으로 추가한다.

---

### [WARNING-2] PRD.md Section 2.4 `requiredQuantity` 설명 모호

- **위치**: `docs/PRD.md` — Section 2.4 `ProductionQueueItem` 테이블
- **설명**:
  PRD Section 2.4의 `requiredQuantity` 설명이 `"부족 수량 (생산 목표)"`로만 기재되어 있어, "주문 수량 - 재고인지" "재고 - 주문 수량의 절댓값인지" 계산 방향이 모호하다.
  PLAN.md Phase 1에는 `"부족 수량 (주문 수량 - 재고, 항상 양수)"`로 명확히 기술되어 있어 두 문서 간 명확성 수준 차이가 있다.
- **권장 조치**: PRD Section 2.4를 `"주문 수량 - 재고 (항상 양수)"`로 구체화하여 PLAN.md와 일치시킨다.

---

### [WARNING-3] CLAUDE.md 현재 상태 표 `docs/PLAN.md` 항목이 "미작성"으로 잔류

- **위치**: `SampleOrderSystem/CLAUDE.md` — "현재 상태 (2026-06-12)" 테이블
- **설명**:
  CLAUDE.md의 현재 상태 표에 `docs/PLAN.md | 미작성`으로 기재되어 있으나, PLAN.md v1.1.0이 이미 작성·수정 완료된 상태이다.
- **권장 조치**: `docs/PLAN.md` 항목을 `작성 완료 (v1.1.0)`으로 업데이트한다.

---

## 통과 항목

- **[A] 교차 참조**: 이상 없음 — PLAN.md의 `[PRD.md](PRD.md)` 링크가 실제 파일과 일치. Phase 별도 파일 없음.
- **[B] 기술 스택**: 이상 없음 — Java 17+, Gradle 8.x, Gson 2.11.0, JUnit Jupiter 6.0.0, JaCoCo 세 문서 모두 일치.
- **[C] 설계 제약 반영**: CRITICAL 2건, WARNING 3건 발견 (위 상세 참조).
- **[D] 완료 기준**: 이상 없음 — Phase 0~5 전체에 완료 기준 명시됨.
- **[E] 내부 모순**: WARNING-1에서 PRD 내부 모순 1건 발견.
