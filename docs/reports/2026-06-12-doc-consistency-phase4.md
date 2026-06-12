# 문서 정합성 검증 보고서

**일시**: 2026-06-12
**검증 문서**:
- `docs/design/phase4.md` (신규)
- `docs/PLAN.md` (v1.1.0)
- `docs/PRD.md` (v1.1.0)
- `SampleOrderSystem/CLAUDE.md`
- `과제/CLAUDE.md`

**결과**: 문제 3건 발견 (CRITICAL: 0, WARNING: 1, INFO: 2)

---

## 발견된 문제

### [WARNING] PLAN.md MonitoringView 변경 범위 과소 기술

- **위치**: `docs/PLAN.md` — Phase 4 출처 요약 표, MonitoringView 행
- **설명**:
  PLAN.md Phase 4 출처 요약 표에서 MonitoringView의 변경 사항이 **"패키지 변경만"** 으로 기술되어 있다.
  그러나 phase4.md Section 1 개요 표 및 Section 7.1에서는 실제 변경 사항이 다음과 같이 더 넓게 명시되어 있다.
  - `%4d sampleId` → `%-8s sampleId` (String ID 대응을 위한 출력 포맷 변경)
  - `clearScreen()` 선택적 적용 (단건 호출 방식 대응)
  - `printFooter` 문구 조정

  두 문서가 동일한 클래스의 이식 범위를 다르게 기술하므로, PLAN.md만 참조하는 구현자가
  실제 필요한 변경 작업을 누락할 가능성이 있다.

- **권장 조치**: PLAN.md Phase 4 출처 요약 표의 MonitoringView 행을 아래와 같이 수정한다.

  ```
  MonitoringView | DataMonitor 이식 | 패키지 변경, String ID 포맷(%4d → %-8s), clearScreen() 선택 적용, printFooter 문구 조정
  ```

---

### [INFO] ReleaseControllerTest 인프라 코드에 미사용 필드 선언

- **위치**: `docs/design/phase4.md` — Section 6.4 ReleaseControllerTest 테스트 인프라
- **설명**:
  테스트 인프라 코드에서 `sampleRepo`(JsonSampleRepository) 및 `queueRepo`(JsonProductionQueueRepository)
  필드가 선언되고 `@BeforeEach`에서 초기화되지만, `ReleaseController` 생성자 시그니처는
  `ReleaseController(OrderRepository orderRepo, ReleaseView view, Scanner scanner)`로
  `SampleRepository`와 `ProductionQueueRepository`를 받지 않는다.
  결과적으로 두 필드는 컨트롤러 생성에 사용되지 않으며, 목적을 알 수 없는 선언으로 구현자에게 혼란을 줄 수 있다.

- **권장 조치**: `sampleRepo`, `queueRepo` 필드를 테스트 인프라 명세에서 제거하거나,
  존치가 필요한 경우(예: 사전 데이터 생성 목적) 주석으로 용도를 명시한다.

---

### [INFO] phase4.md MonitoringView ANSI 상수 명세에서 주문 상태 색상 누락

- **위치**: `docs/design/phase4.md` — Section 3.1 ANSI 상수
- **설명**:
  PLAN.md Phase 4 설계 상세 MonitoringView 항목에는
  "상태별 주문 수: RESERVED(파란), PRODUCING(주황), CONFIRMED(초록), RELEASE(보라) 색상 적용"이 명시되어 있다.
  그러나 phase4.md Section 3.1 ANSI 상수 명세에는 `ANSI_YELLOW`와 `ANSI_RED`만 정의되어 있고,
  파란(BLUE), 초록(GREEN), 보라(MAGENTA) 상수가 누락되어 있다.
  DataMonitor 원본을 그대로 이식하면 해당 상수가 포함되겠지만,
  phase4.md 명세만으로는 주문 상태별 색상 적용 여부를 확인할 수 없다.

- **권장 조치**: phase4.md Section 3.1에 주문 상태 색상에 필요한 ANSI 상수를 보완 명시한다.

  ```java
  private static final String ANSI_BLUE    = "\033[34m";
  private static final String ANSI_GREEN   = "\033[32m";
  private static final String ANSI_MAGENTA = "\033[35m";
  ```

---

## 통과 항목

- **[A] 교차 참조**: 이상 없음
  - PLAN.md Phase 4 산출물 목록(MonitoringController, MonitoringView, ReleaseController, ReleaseView)과 phase4.md 신규 파일 목록 일치
  - phase4.md 선행 Phase(Phase 1)가 PLAN.md Phase 4 선행 Phase와 일치
  - FR 번호(FR-MON-01·02, FR-REL-01)가 PRD.md Section 3.4, 3.5에 모두 존재하고 내용 일치

- **[B] 기술 스택**: 이상 없음
  - Java 17+, Gradle 8.x, Gson 2.11.0, JUnit Jupiter 6.0.0 전 문서 일치
  - phase4.md에서 CLAUDE.md 미정의 신규 외부 의존성 없음

- **[C] 설계 제약 반영**: 이상 없음 (CRITICAL 없음)
  - MVC 레이어 규칙 준수 확인 (View에서 Model 직접 수정 없음)
  - 생성자 주입 패턴 준수 (MonitoringController, ReleaseController 모두 생성자 주입)
  - 수율 계산식은 ProductionLineController에만 위치 (phase4.md에서 해당 로직 미포함 — 정상)
  - ReleaseController가 SampleRepository 미주입 설계가 PLAN.md와 일치

- **[D] 완료 기준**: 이상 없음
  - phase4.md Section 8에 완료 기준 명시
  - FR-MON-01·02 (REJECTED 제외, 재고 상태 3종), FR-REL-01 (CONFIRMED→RELEASE 전이) 검증 항목 포함

- **[E] 내부 모순**: 문제 1건 (WARNING으로 보고됨)
  - MonitoringView 변경 범위 기술 불일치 (PLAN.md "패키지 변경만" vs phase4.md 실제 4가지 변경)
  - calcStockLevel 로직은 PRD.md FR-MON-02 재고 상태 판정 기준과 일치 (stock==0 고갈, stock<demandSum 부족, 그 외 여유)
  - getSampleSummary() 역할 설명이 PLAN.md Router 설계 및 Phase 5 Router 상세와 일치
  - ID 타입(String) 규칙이 전 문서에서 일관되게 적용됨 (SampleStatus record, StubRepository 모두 String ID)
