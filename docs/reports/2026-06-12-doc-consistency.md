# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `과제/CLAUDE.md` (상위 프로젝트 CLAUDE.md)
- `SampleOrderSystem/CLAUDE.md` (프로젝트 CLAUDE.md)
- `SampleOrderSystem/docs/PRD.md`

**결과**: 문제 6건 발견 (CRITICAL: 1, WARNING: 4, INFO: 1)

---

## 발견된 문제

### [CRITICAL] 생산량 계산 책임 위치 충돌

- **위치**: `SampleOrderSystem/CLAUDE.md` — 섹션 "이 프로젝트에서 강조하는 제약" 4번  
  vs `SampleOrderSystem/docs/PRD.md` — FR-ORD-02 주문 승인
- **설명**: 프로젝트 CLAUDE.md는 수율 계산식 `ceil(부족분 / (yield × 0.9))`를 `ProductionLineController` 도메인 로직에만 위치하도록 명시한다. 그러나 PRD.md FR-ORD-02는 주문 승인 처리 흐름 내에서 동일 계산식을 직접 기술하고 있어, 구현자가 이 로직을 `OrderController`에 작성하도록 유도할 수 있다. 두 문서가 동일 로직의 책임 소재를 다른 Controller로 암시하는 모순이다.
- **권장 조치**: PRD.md FR-ORD-02에 "(계산 수행 주체는 ProductionLineController에 위임)" 주석을 추가하거나, 계산식 기술을 FR-PROD 섹션으로 이동하여 책임 소재를 명확히 한다.

---

### [WARNING] 모니터링 PoC 리포지터리 이름 불일치

- **위치**: `과제/CLAUDE.md` — 리포지터리 구성 테이블, PoC → 본 프로젝트 연계 테이블  
  vs `SampleOrderSystem/CLAUDE.md` — PoC 통합 전략 테이블  
  vs `SampleOrderSystem/docs/PRD.md` — 섹션 6 PoC 통합 계획 테이블
- **설명**: 상위 CLAUDE.md는 모니터링 PoC 이름을 `DataMonitoring`으로 표기한다. 반면 프로젝트 CLAUDE.md와 PRD.md는 동일 PoC를 `DataMonitor`로 표기한다. 세 문서 간 이름이 일치하지 않아 실제 리포지터리 디렉터리명과 혼동될 수 있다.
- **권장 조치**: 실제 리포지터리 디렉터리명을 기준으로 세 문서 모두 동일한 이름으로 통일한다.

---

### [WARNING] 상위 CLAUDE.md 패키지 구조에 SampleStatus.java 누락

- **위치**: `과제/CLAUDE.md` — 패키지 구조 섹션  
  vs `SampleOrderSystem/CLAUDE.md` — 패키지 구조 섹션
- **설명**: 프로젝트 CLAUDE.md는 `model/entity/SampleStatus.java (DataMonitor 이식, record)`를 패키지 구조에 포함한다. 상위 CLAUDE.md의 패키지 구조에는 이 파일이 없다. 상위 CLAUDE.md가 전체 규칙의 기준 문서이므로, 신규 파일이 추가될 경우 상위 문서에도 반영되어야 한다.
- **권장 조치**: 상위 CLAUDE.md의 패키지 구조에 `SampleStatus.java`를 추가하거나, 서브 프로젝트에서 확장 가능한 항목임을 상위 문서에 명시한다.

---

### [WARNING] REJECTED 상태 설명의 "모니터링 제외" 문구 누락

- **위치**: `과제/CLAUDE.md` — 주문 상태 (OrderStatus) 섹션  
  vs `SampleOrderSystem/docs/PRD.md` — 섹션 2.3 주문 상태
- **설명**: 상위 CLAUDE.md는 `REJECTED → 주문 거절 (모니터링 제외)`로 명시하여 REJECTED 상태 주문이 모니터링에서 제외됨을 정의한다. PRD.md는 `REJECTED → 주문 거절`로만 기술하고 있어 이 규칙이 누락되어 있다. PRD.md FR-MON-01에서는 "`REJECTED` 제외한 상태별 주문 수 표시"라고 명시하고 있으나, 상태 정의 테이블 자체에는 이 제약이 빠져 있다.
- **권장 조치**: PRD.md 섹션 2.3의 REJECTED 상태 설명에 "(모니터링 제외)" 문구를 추가하여 상위 CLAUDE.md와 일치시킨다.

---

### [WARNING] 상위 CLAUDE.md 도메인 모델에 ProductionQueueItem 엔티티 누락

- **위치**: `과제/CLAUDE.md` — 도메인 모델 섹션  
  vs `SampleOrderSystem/docs/PRD.md` — 섹션 2.4 생산 큐 항목 (ProductionQueueItem)
- **설명**: PRD.md는 `ProductionQueueItem` 엔티티를 속성 7개(queueId, orderId, sampleId, requiredQuantity, actualProductionQuantity, totalProductionTime, enqueuedAt)와 함께 상세 정의한다. 상위 CLAUDE.md의 도메인 모델 섹션에는 이 엔티티가 없다. 상위 CLAUDE.md가 도메인 모델의 기준 문서인 만큼, 신규 엔티티 추가 시 상위 문서에도 반영이 필요하다.
- **권장 조치**: 상위 CLAUDE.md 도메인 모델 섹션에 `ProductionQueueItem` 정의를 추가한다.

---

### [INFO] sampleId 타입 표기 불일치 (상위 CLAUDE.md만 미확정)

- **위치**: `과제/CLAUDE.md` — 도메인 모델 Sample 테이블  
  vs `SampleOrderSystem/CLAUDE.md` — 이식 시 주의사항 3번  
  vs `SampleOrderSystem/docs/PRD.md` — 섹션 2.1 Sample 테이블
- **설명**: 상위 CLAUDE.md는 `sampleId` 및 `orderId` 타입을 `String/Long`으로 표기하여 타입이 확정되지 않은 상태이다. 프로젝트 CLAUDE.md와 PRD.md는 모두 `String`으로 확정하고 있다. 기술적 충돌은 아니나, 상위 문서의 미확정 표기가 혼란을 줄 수 있다.
- **권장 조치**: 상위 CLAUDE.md의 Sample, Order 도메인 모델 타입을 `String`으로 확정하여 하위 문서와 일치시킨다.

---

## 통과 항목

- [A] 교차 참조 (Phase 문서 링크): 해당 없음 — PLAN.md 및 phase 문서 미작성 상태
- [B] 기술 스택 일관성: 이상 없음 — Java 17+, Gradle 8.x, Gson 2.11.0, JUnit Jupiter 6.0.0 세 문서 간 일치
- [C] 설계 제약 반영 (수정 금지 등): 문제 1건 (CRITICAL — 계산 책임 위치 충돌)
- [D] 완료 기준 (acceptance criteria): 해당 없음 — Phase 문서 미작성 상태
- [E] 내부 모순: 문제 5건 (WARNING 4, INFO 1)
- 상태 전이 규칙: 이상 없음 — 세 문서 간 전이 조건 및 결과 상태 일치
- MVC 레이어 규칙: 이상 없음
- 코딩 컨벤션: 이상 없음
