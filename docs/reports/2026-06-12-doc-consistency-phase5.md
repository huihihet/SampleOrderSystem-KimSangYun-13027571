# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `docs/design/phase5.md`
- `docs/PLAN.md`
- `docs/PRD.md`
- `SampleOrderSystem/CLAUDE.md`
- `과제/CLAUDE.md`

**결과**: 문제 3건 발견 (CRITICAL: 1, WARNING: 1, INFO: 1)

---

## 발견된 문제

### [CRITICAL] PRD Section 6.5 — SampleFixture·OrderFixture ID 포맷 불일치

- **위치**: `docs/PRD.md` — Section 6.5 (DummyDataGenerator 이식 표, 302~303번 줄)
- **설명**:
  PRD 6.5는 SampleFixture ID 포맷을 `"S001"`, OrderFixture ID 포맷을 `"O001"`로 명시하고 있다.
  그러나 아래 4개 문서는 모두 하이픈 포함 포맷을 정의한다.

  | 문서 | 시료 ID 포맷 | 주문 ID 포맷 |
  |------|------------|------------|
  | PRD Section 8.1 (ID 형식 표) | `S-NNN` (`S-001`) | `ORD-YYYYMMDD-NNNN` |
  | CLAUDE.md (SampleOrderSystem) | `"S-001"` | `"ORD-YYYYMMDD-NNNN"` |
  | PLAN.md Phase 5 SampleFixture | `"S-001"` | — |
  | PLAN.md Phase 5 OrderFixture | — | `"ORD-20260101-0001"` |
  | phase5.md Section 6 (SampleFixture 코드) | `"S-" + String.format("%03d", i)` | — |
  | phase5.md Section 7 (OrderFixture 코드) | — | `"ORD-20260101-" + String.format("%04d", i)` |

  PRD 6.5의 `"S001"`, `"O001"` 포맷은 PRD 8.1 정의와도 자기 모순이며,
  이를 그대로 구현하면 SampleFixtureTest의 `generate_ID_포맷_S_NNN` 테스트가 실패한다.

- **권장 조치**: `docs/PRD.md` Section 6.5 표에서 ID 포맷을 아래와 같이 수정한다.
  - `SampleGenerator` 행: `"S001"` -> `"S-001"` 포맷
  - `OrderGenerator` 행: `"O001"` -> `"ORD-YYYYMMDD-NNNN"` 포맷 (픽스처 고정값 예시: `"ORD-20260101-0001"`)

---

### [WARNING] PLAN.md 628번 줄 — Router 메뉴 수 오기입(5개 vs 6개) 내부 모순

- **위치**: `docs/PLAN.md` — Phase 5 설계 상세 Router 섹션, 628번 줄
- **설명**:
  628번 줄에 "ConsoleMVC의 `Router`를 **5개 메뉴**로 확장한다."라고 기술되어 있다.
  그러나 동일 파일 내에서도 다음과 같이 6개 메뉴를 전제한다.

  - 734번 줄: `RouterTest` 검증 내용 — "**6개** 메뉴 각각 올바른 Controller 호출"
  - 735번 줄: `MainViewTest` 검증 내용 — "**6개** 메뉴 항목"

  또한 PRD Section 3.1 메인 메뉴 정의(`[1]`~`[6]`), PRD Section 6.2 Router 행
  ("5개 Controller 주입, **6개** 메뉴로 확장"), phase5.md route 1~6 모두 6개 메뉴이다.
  결과적으로 PLAN.md 628번 줄만 "5개"로 남아 문서 내 모순이 발생한다.

- **권장 조치**: `docs/PLAN.md` 628번 줄을 다음과 같이 수정한다.
  - 수정 전: "ConsoleMVC의 `Router`를 5개 메뉴로 확장한다."
  - 수정 후: "ConsoleMVC의 `Router`를 6개 메뉴로 확장한다."

---

### [INFO] PLAN.md Phase 5 — `handleSubMenu()` / `getOrderCount()` / `getQueueWaitingCount()` 메서드 명세 누락

- **위치**: `docs/PLAN.md` — Phase 5 설계 상세 (기존 Controller 메서드 추가 항목 없음)
- **설명**:
  phase5.md는 기존 4개 Controller에 다음 메서드를 추가하는 것으로 설계한다.

  | 클래스 | 추가 메서드 |
  |--------|-----------|
  | `SampleController` | `handleSubMenu()` |
  | `OrderController` | `handleSubMenu()` |
  | `ProductionLineController` | `handleSubMenu()`, `getQueueWaitingCount()` |
  | `MonitoringController` | `getOrderCount()` |

  PLAN.md Phase 5 설계 상세에는 이 메서드들이 명시되어 있지 않다.
  Router `run()` 주석에 "서브메뉴 루프"가 암시되어 있을 뿐이다.
  설계 충돌은 아니나 PLAN.md만 보고 구현할 경우 해당 메서드의 존재를 추론해야 하며
  추적성이 불완전하다.

- **권장 조치**: `docs/PLAN.md` Phase 5 설계 상세에 "기존 Controller 메서드 추가" 항목을
  phase5.md Section 1의 표 수준으로 추가하여 추적성을 확보한다. 구현을 차단하는 수준의 문제는 아님.

---

## 통과 항목

- **[A] 교차 참조 일관성**: 이상 없음
  - PLAN.md Phase 구성(0~5)과 실제 phase 파일(phase5.md) 존재 확인됨
  - PLAN.md의 `[PRD.md](PRD.md)` 링크 경로 정상
- **[B] 기술 스택 일관성**: 이상 없음
  - Gson 2.11.0, JUnit Jupiter 6.0.0, Gradle 8.x, Java 17+ — 모든 문서 일치
  - phase5.md에서 새로운 외부 의존성 추가 없음
- **[C] 설계 제약 반영**: 이상 없음
  - CLAUDE.md MVC 레이어 규칙 준수 (View 직접 출력만, Controller 도메인 로직 없음)
  - `Main.java`는 DI 조립만 담당 — CLAUDE.md 제약 준수
  - 수율 계산식이 `ProductionLineController`에만 위치 — CLAUDE.md 제약 준수
  - Entity ID 타입 String 통일 — CLAUDE.md 제약 준수
- **[D] 완료 기준**: 이상 없음
  - phase5.md Section 10에 완료 기준 4개 항목 명시됨
- **[E] Router 생성자 파라미터 순서**: 이상 없음
  - PLAN.md, phase5.md Section 4.1, phase5.md Section 5(Main 코드) 모두 동일 순서
    `(SampleController, OrderController, MonitoringController, ReleaseController, ProductionLineController, MainView, Scanner)`
- **[F] 6개 메뉴 라우팅 vs PRD 3.1**: 이상 없음
  - phase5.md route 1~6 매핑이 PRD 3.1 메뉴 정의와 완전 일치
- **[G] MainView.printMainMenu 파라미터**: 이상 없음
  - PLAN.md와 phase5.md 모두 `(int totalSamples, long totalStock, long totalOrders, long prodLineWaiting)` 동일
- **[H] SampleFixture / OrderFixture ID 포맷 (PLAN.md vs phase5.md)**: 이상 없음
  - PLAN.md 예시 `"S-001"`, `"ORD-20260101-0001"` — phase5.md 구현 코드와 일치
- **[I] 통합 테스트 4개 시나리오**: 이상 없음
  - PLAN.md Phase 5 시나리오 1~4와 phase5.md Section 8.5 시나리오 1~4 완전 일치
