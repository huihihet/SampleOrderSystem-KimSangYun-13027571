# 문서 정합성 검증 보고서

**일시**: 2026-06-12
**검증 문서**:
- `C:\reviewer\workspace\과제\CLAUDE.md` (상위 프로젝트 CLAUDE.md)
- `C:\reviewer\workspace\과제\SampleOrderSystem\CLAUDE.md` (서브 프로젝트 CLAUDE.md)
- `C:\reviewer\workspace\과제\SampleOrderSystem\docs\PRD.md`
- `C:\reviewer\workspace\과제\SampleOrderSystem\docs\PLAN.md`

**결과**: 문제 5건 발견 (CRITICAL: 2, WARNING: 2, INFO: 1)

---

## 발견된 문제

### [CRITICAL-1] Controller에서 도메인 로직(재고 차감) 직접 수행 — 레이어 규칙 위반

- **위치**: `PLAN.md` — Phase 3 `OrderController.approveOrder()` 설계 상세
- **설명**: PLAN.md의 `approveOrder()` 설계에 "재고 충분: stock 차감, 상태 CONFIRMED"가 명시되어 있다. `stock` 차감은 도메인 상태 변경 로직이며, CLAUDE.md 레이어 규칙에서 Controller 레이어의 금지 사항인 "도메인 로직"에 해당한다. 재고 차감은 Model(Entity 또는 Repository) 레이어에서 처리해야 한다.
- **권장 조치**: `Sample` 엔티티에 `decreaseStock(int quantity)` 메서드를 추가하거나, `SampleRepository.update(sample)` 호출 전에 Model 레이어에서 재고 변경을 수행하도록 설계를 수정한다. Controller는 Model 호출 후 View 위임만 담당해야 한다.

---

### [CRITICAL-2] Router(앱 조립 레이어)에서 비즈니스 로직(재고 상태 집계) 직접 수행 — 레이어 규칙 위반

- **위치**: `PLAN.md` — Phase 5 `Router` 설계 상세, `run()` 메서드
- **설명**: "시료 요약 계산: `sampleRepo.findAll()`에서 각 시료의 재고 상태를 집계"가 `Router.run()` 내에 위치하도록 설계되어 있다. 재고 상태 집계(총 수·부족·고갈 개수 카운팅)는 비즈니스 로직에 해당하며, `Router`는 CLAUDE.md에서 "메뉴 루프와 Controller 위임만 담당"하도록 명시되어 있어 해당 역할 범위를 벗어난다.
- **권장 조치**: 재고 상태 집계 로직을 `MonitoringController` 또는 `SampleController`의 메서드로 이동하고, `Router`는 해당 메서드의 반환값을 `MainView`에 전달하는 역할만 수행하도록 수정한다.

---

### [WARNING-1] `calcStockLevel` 경계값 처리 기준 — PRD와 PLAN 간 모호성

- **위치**: `PRD.md` — Section 3.4 FR-MON-02 / `PLAN.md` — Phase 4 `MonitoringController` 설계 및 테스트 케이스
- **설명**: PRD.md에서 재고 상태 판정은 "여유: 주문 수량 합산 대비 충분", "부족: 주문 대비 부족"으로 정의되어 있으나 `stock == demandSum`인 경계 상황을 명시하지 않는다. PLAN.md의 테스트 케이스에는 `stock == demand → 여유`로 명시되어 있다. 구현 시 이 경계값 해석이 PRD 의도와 일치하는지 확인이 필요하다.
- **권장 조치**: PRD.md FR-MON-02에 경계 조건을 명시적으로 추가한다. 예: "여유: 재고 ≥ 주문 수량 합산 (재고와 수요가 같은 경우 포함)".

---

### [WARNING-2] `ProductionQueueItem.requiredQuantity` 필드 설명 불일치

- **위치**: `PLAN.md` — Phase 1 Entity `ProductionQueueItem` 설계 상세 및 Phase 3 `createQueueItem()` 설계
- **설명**: Phase 1의 Entity 필드 주석에는 `requiredQuantity`가 "부족 수량 (재고 - 주문 수량의 절댓값)"으로 표현되어 있다. 그러나 Phase 3의 계산식은 `requiredQuantity = order.getQuantity() - sample.getStock()` (주문 수량 - 재고)이다. 두 표현이 절댓값으로는 동일하지만 "재고 - 주문 수량"과 "주문 수량 - 재고"는 부호가 반대여서 혼란을 유발한다. 재고 부족 상황(재고 < 주문 수량)에서 Phase 3 계산식은 양수이므로 실제 의미상으로는 Phase 3이 올바르다.
- **권장 조치**: Phase 1 Entity 필드 주석을 `"부족 수량 (주문 수량 - 재고, 항상 양수)"`로 수정하여 Phase 3 계산식과 일치시킨다.

---

### [INFO-1] 상위 CLAUDE.md 패키지 구조 테이블에 `SampleStatus.java`, `JsonFileUtil.java` 누락

- **위치**: `C:\reviewer\workspace\과제\CLAUDE.md` — "패키지 구조 (SSemi 기준)" 섹션
- **설명**: 상위 CLAUDE.md의 패키지 구조 트리에 `model/entity/SampleStatus.java`와 `model/repository/JsonFileUtil.java`가 누락되어 있다. 두 파일은 SampleOrderSystem CLAUDE.md의 패키지 구조와 PLAN.md Phase 1 신규 파일 목록에는 포함되어 있다. 상위 문서가 서브 프로젝트에 의해 확장되는 구조이므로 기능적 오류는 아니나, 상위 문서가 불완전한 패키지 구조를 제시하여 독자에게 혼선을 줄 수 있다.
- **권장 조치**: 상위 CLAUDE.md의 패키지 구조 트리에 `SampleStatus.java`(DataMonitor 이식)와 `JsonFileUtil.java`(DataPersistence 이식)를 추가한다.

---

## 통과 항목

- **[A] 교차 참조**: 이상 없음 — PLAN.md의 Phase별 파일 목록이 SampleOrderSystem CLAUDE.md 패키지 구조와 일치. 상위 CLAUDE.md 패키지 구조 누락은 INFO-1로 별도 기록.
- **[B] 기술 스택**: 이상 없음 — Gson 2.11.0, JUnit Jupiter 6.0.0, Gradle 8.x, Java 17+이 PRD·PLAN·CLAUDE.md 전체에서 일관되게 사용됨. 상위 CLAUDE.md의 "5.x (또는 6.x)" 허용 범위 내 선택임.
- **[C] 설계 제약**: 문제 2건 (CRITICAL-1, CRITICAL-2) — Controller 및 Router의 레이어 규칙 위반 설계
- **[D] 완료 기준**: 이상 없음 — Phase 0~5 모두 완료 기준(acceptance criteria) 명시됨
- **[E] 내부 모순**: 문제 2건 (WARNING-1, WARNING-2) — calcStockLevel 경계값 모호성, requiredQuantity 필드 설명 불일치
