# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `과제/CLAUDE.md` (상위 CLAUDE.md)
- `과제/SampleOrderSystem/CLAUDE.md` (프로젝트 CLAUDE.md)
- `과제/SampleOrderSystem/docs/PRD.md`

**결과**: 문제 7건 발견 (CRITICAL: 0, WARNING: 5, INFO: 2)

---

## 발견된 문제

### [WARNING-1] JUnit 버전 표기 불일치

- **위치**: `과제/CLAUDE.md` — 기술 스택 섹션
- **설명**: 상위 CLAUDE.md는 테스트 프레임워크를 `JUnit Jupiter 5.x (또는 6.x)`로 범위 표기한다. 반면 SampleOrderSystem CLAUDE.md의 `build.gradle` 필수 추가 항목과 PRD.md 5절은 `junit-bom:6.0.0` / `JUnit Jupiter 6.0.0`으로 버전을 확정하였다. 상위 문서가 하위 결정을 반영하지 않아 혼동 유발이 가능하다.
- **권장 조치**: 상위 CLAUDE.md의 기술 스택 표기를 `JUnit Jupiter 6.x`로 업데이트하거나, 하위 문서에서 선택 근거를 주석으로 명시한다.

---

### [WARNING-2] JSON 라이브러리 표기 불일치

- **위치**: `과제/CLAUDE.md` — 기술 스택 섹션
- **설명**: 상위 CLAUDE.md는 JSON 라이브러리를 `Gson 또는 Jackson`으로 선택지를 열어 두었다. SampleOrderSystem CLAUDE.md와 PRD.md는 `Gson 2.11.0`으로 확정하였다. 상위 문서가 최신 결정을 반영하지 않고 있다.
- **권장 조치**: 상위 CLAUDE.md의 기술 스택 섹션을 `Gson 2.11.0`으로 확정 표기하거나, 하위 문서에 "상위 문서의 선택지 중 Gson을 채택"이라는 근거를 명시한다.

---

### [WARNING-3] 도메인 모델 ID 타입 불일치

- **위치**: `과제/CLAUDE.md` — 도메인 모델 섹션 (Sample, Order 테이블)
- **설명**: 상위 CLAUDE.md는 `sampleId`와 `orderId`의 타입을 `String/Long`으로 표기한다. SampleOrderSystem CLAUDE.md의 "핵심 설계 결정 사항" 및 PRD.md 2.1~2.2절은 두 ID를 `String`으로 통일하였다. 상위 문서의 `String/Long` 이중 표기가 구현 시 혼동을 일으킬 수 있다.
- **권장 조치**: 상위 CLAUDE.md 도메인 모델의 ID 타입을 `String`으로 수정한다.

---

### [WARNING-4] SampleRepository 인터페이스 명세와 PRD 기능 범위 불일치

- **위치**: `과제/SampleOrderSystem/CLAUDE.md` — 인터페이스 최종 명세 (SampleRepository)
- **설명**: SampleOrderSystem CLAUDE.md의 `SampleRepository` 인터페이스 명세에 `update(Sample sample)`과 `deleteById(String sampleId)` 메서드가 포함되어 있다. 그러나 동일 문서의 PoC 이식 계획에서 `SampleController`의 `update`/`delete` 기능은 "PRD 범위 외"라는 이유로 제거 대상으로 명시되어 있다. PRD.md 3.2절(시료 관리)에도 시료 수정·삭제 기능은 포함되지 않는다. 인터페이스에 메서드를 유지하는 설계 의도(향후 확장 대비 등)가 어디에도 명시되어 있지 않다.
- **권장 조치**: 인터페이스 명세에서 `update`/`deleteById`를 제거하거나, 유지하는 경우 그 근거(예: "향후 확장 대비 예약")를 명세 내에 주석으로 명시한다.

---

### [WARNING-5] OrderRepository 인터페이스 명세와 PRD 기능 범위 불일치

- **위치**: `과제/SampleOrderSystem/CLAUDE.md` — 인터페이스 최종 명세 (OrderRepository)
- **설명**: `OrderRepository` 인터페이스 명세에 `update(Order order)`와 `deleteById(String orderId)` 메서드가 포함되어 있다. PRD.md 3.3절(주문 처리)에는 주문 수정·삭제 기능이 없으며, 상태 전이는 `update` 호출로 구현되겠지만 `deleteById`는 어떤 기능 요구사항에도 대응하지 않는다. WARNING-4와 동일한 근거 미명시 문제이다.
- **권장 조치**: `deleteById`를 인터페이스 명세에서 제거하거나, 유지 근거를 명시한다. `update`는 상태 전이 구현에 필요하므로 유지하되 그 목적을 명세에 기술한다.

---

### [INFO-1] 워크플로우 규칙 — 전역 CLAUDE.md와 SampleOrderSystem CLAUDE.md 간 충돌

- **위치**: `과제/SampleOrderSystem/CLAUDE.md` — 워크플로우 규칙 섹션
- **설명**: SampleOrderSystem CLAUDE.md는 `doc-consistency` 완료 후 `test-verify` + `compliance-verify`를 병렬 실행한다고 명시한다. 그러나 전역 CLAUDE.md(`C:/Users/User/.claude/CLAUDE.md`)는 `test-verify`와 `compliance-verify`를 "자동 실행 비활성화 — 사용자가 명시적으로 호출할 때만 실행"으로 규정한다. 두 규칙이 충돌하며, 전역 규칙이 우선 적용되므로 SampleOrderSystem CLAUDE.md의 해당 항목이 사실상 무효화된다.
- **권장 조치**: SampleOrderSystem CLAUDE.md의 워크플로우 규칙 2번 항목을 삭제하거나 전역 규칙과 일치하도록 "사용자 명시적 호출 시 실행"으로 수정한다.

---

### [INFO-2] ProductionQueueItem 속성명 불일치

- **위치**: `과제/SampleOrderSystem/CLAUDE.md` — 신규 작성 목록 (ProductionQueueItem 설명) vs `과제/SampleOrderSystem/docs/PRD.md` — 2.4절
- **설명**: PRD.md 2.4절은 생산 큐 항목의 속성명을 `requiredQuantity`(부족 수량, 생산 목표)와 `actualProductionQuantity`(실 생산량)으로 정의한다. SampleOrderSystem CLAUDE.md의 신규 작성 목록에서는 동일 속성을 `requiredQty`, `actualQty`로 약칭하여 표기한다. 구현 시 어느 명칭을 기준으로 삼을지 혼란이 생길 수 있다.
- **권장 조치**: 두 문서 중 하나를 기준으로 통일한다. PRD가 공식 명세이므로 PRD의 `requiredQuantity` / `actualProductionQuantity`를 기준으로 SampleOrderSystem CLAUDE.md를 수정하는 것을 권장한다.

---

## 통과 항목

- **[A] 교차 참조**: 이상 없음 — 상속 구조 명시, 파일 링크 사용 없음
- **[B] 기술 스택**: 문제 2건 (WARNING-1 JUnit 버전, WARNING-2 JSON 라이브러리 상위 문서 미반영)
- **[C] 설계 제약 반영**: 이상 없음 — "수정 금지" 등 명시적 제약 위반 없음
- **[D] 완료 기준**: 해당 없음 — 검증 대상 문서는 PRD이며 Phase 문서 아님
- **[E] 내부 모순**: 문제 5건 (WARNING-3 ID 타입, WARNING-4~5 인터페이스 메서드 범위 초과, INFO-1 워크플로우 충돌, INFO-2 속성명 약칭)

---

## 종합 의견

CRITICAL 등급 문제는 없으며 시스템 설계의 핵심 흐름(상태 전이, 생산 수율 계산식, MVC 레이어 규칙, PoC 이식 방향)은 세 문서 간에 일관되게 기술되어 있다. 발견된 문제는 모두 상위 문서의 미반영 또는 인터페이스 명세 범위 불명확에서 비롯된 것으로, 구현 전에 정리해 두면 이후 코드 생성 및 검증 단계에서의 혼선을 줄일 수 있다.
