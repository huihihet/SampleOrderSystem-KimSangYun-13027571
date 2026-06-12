# 컴플라이언스 검증 보고서

**일시**: 2026-06-12
**검증 대상**: `docs/PLAN.md` (v1.1.0)
**결과**: 위반 4건 (CRITICAL: 1, WARNING: 3)

---

## 발견된 위반

### [CRITICAL] RouterTest 설명이 6개 메뉴 체계와 불일치

- **위치**: `docs/PLAN.md` — Phase 5 테스트 계획 표
- **위반 규칙**: 설계 내 일관성 — Phase 5 테스트 계획의 `RouterTest` 설명이 `route()` 메서드 설계 및 PRD Section 3.1과 충돌함
- **현재 설계**:
  - `Router.route()` 메서드 설계: `1~6 → 각 Controller, 0 → false(종료)` (6개 메뉴 체계)
  - PRD Section 3.1: `[1]~[6] + [0] 종료` (6개 메뉴)
  - RouterTest 설명: `"5개 메뉴 각각 올바른 Controller 호출, menu=0 종료"` (5개 메뉴)
  - `MainViewTest` 설명: `"시료 요약 포함, 5개 메뉴 항목"` (5개 메뉴)
- **영향**: RouterTest 작성 시 메뉴 6번(출고 처리)에 대한 테스트가 누락될 수 있으며, MainViewTest도 `[6] 출고 처리` 항목 검증이 빠질 위험이 있음
- **권장 수정**: Phase 5 테스트 계획 표의 RouterTest 설명을 `"6개 메뉴 각각 올바른 Controller 호출, menu=0 종료"`로, MainViewTest 설명을 `"시료 요약 포함, 6개 메뉴 항목"`으로 수정

---

### [WARNING] Router.run()에 집계 로직이 포함됨 — 레이어 분리 경계 모호

- **위치**: `docs/PLAN.md` — Phase 5 설계 상세, `Router` 섹션
- **위반 규칙**: CLAUDE.md 아키텍처 레이어 규칙 — Controller는 Model 호출·View 위임·입력 파싱을 담당, Router는 "메뉴 루프와 Controller 위임만 담당"
- **현재 설계**: `run()` 메서드 설명에 `"getSampleSummary() 호출 → 결과를 집계하여 mainView.printMainMenu() 파라미터로 전달"` 이라고 명시되어 있어, Router 내부에서 `getSampleSummary()` 반환값으로부터 `totalSamples`, `totalStock`, `totalOrders`, `prodLineWaiting` 등을 집계하는 로직이 Router에 위치하게 됨
- **영향**: 집계 연산(stream 합산 등)이 Router에 들어갈 경우 "메뉴 루프와 Controller 위임만 담당"이라는 Router 역할 정의를 위반함. Router가 얇은 조정자(thin coordinator)로 유지되어야 하는 원칙에 반함
- **권장 수정**: `MonitoringController`에 `MainMenuSummary` VO 또는 개별 집계 메서드(`getTotalStock()`, `getProdLineWaiting()` 등)를 추가하거나, `getSampleSummary()`와 함께 집계 결과를 담은 단일 객체를 반환하도록 변경하여 Router는 해당 값을 그대로 `mainView.printMainMenu()`에 전달만 하도록 설계 조정

---

### [WARNING] Order.changeStatus()가 setStatus()와 기능 중복 — 불필요한 추상화

- **위치**: `docs/PLAN.md` — Phase 1 설계 상세, `Entity: Order` 섹션
- **위반 규칙**: CLAUDE.md 코딩 컨벤션 — "단 3개 이하의 유사한 코드에 추상화를 적용하지 않는다", 불필요한 복잡성 금지
- **현재 설계**: `void changeStatus(OrderStatus newStatus)` 의 구현이 `this.status = newStatus` 한 줄로, Gson 역직렬화를 위해 이미 존재하는 `setStatus(OrderStatus status)` setter와 완전히 동일한 역할을 함
- **영향**: 동일한 기능을 수행하는 메서드가 두 개 공존하면 구현 시 일관성 없이 혼용될 수 있고, 추가 테스트 부담이 생김
- **권장 수정**: `changeStatus()` 메서드를 제거하고 `setStatus()` setter를 직접 사용하도록 설계 변경. 상태 전이 규칙 강제가 필요하다면 허용 전이를 검사하는 로직을 포함시켜 `setStatus()`와 명확히 차별화할 것

---

### [WARNING] JsonOrderRepository.findBySampleId() 내부 null 방어 코드 — 내부 API 과잉 검증

- **위치**: `docs/PLAN.md` — Phase 1 설계 상세, `JsonOrderRepository` 섹션
- **위반 규칙**: CLAUDE.md 보안 규칙 — "사용자 입력 경계에서만 검증, 내부 API 불필요한 검증 금지"
- **현재 설계**: `if (sampleId == null) throw new IllegalArgumentException("sampleId는 null일 수 없습니다.")` 검증이 Repository 구현체(내부 레이어) 내부에 명시되어 있음
- **영향**: `sampleId`는 Controller 레이어에서 사용자 입력을 받아 파싱한 뒤 전달되므로 Repository에 null이 도달하는 경로가 없음. CLAUDE.md가 금지하는 "발생 불가한 예외 처리" 및 내부 API 불필요한 검증에 해당
- **권장 수정**: `findBySampleId()` 내부의 null 검증 코드 제거. null 방어가 필요하다면 사용자 입력을 받는 Controller 또는 View 레이어의 경계에서 처리

---

## 검증 결과 요약

- [A] 아키텍처 제약: 경고 있음 (Router 집계 로직 경계 모호)
- [B] 코딩 컨벤션: 경고 있음 (Order.changeStatus() 중복, RouterTest 기술 오류)
- [C] 보안: 경고 있음 (내부 API 과잉 검증)
- [D] 불필요한 복잡성: 경고 있음 (changeStatus() 추상화, RouterTest 불일치)
