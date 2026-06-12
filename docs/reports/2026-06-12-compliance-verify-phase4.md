# 컴플라이언스 검증 보고서

**일시**: 2026-06-12
**검증 대상**: `docs/design/phase4.md` (Phase 4 — 모니터링 + 출고 처리 기능)
**결과**: 위반 3건 (CRITICAL: 0, WARNING: 3)

---

## 발견된 위반

### [WARNING] WHAT 주석 — `// FR-MON-01·02 단건 렌더링` 및 `// private`

- **위치**: `phase4.md` — Section 2.2 (MonitoringController 메서드 명세)
- **위반 규칙**: CLAUDE.md 코딩 컨벤션 — "주석: WHY가 비자명한 경우에만 한 줄 이내로 작성"
- **현재 설계**:
  ```java
  // FR-MON-01·02 단건 렌더링
  void showMonitoring()

  // private
  String calcStockLevel(int stock, int demandSum)
  ```
  `// FR-MON-01·02 단건 렌더링`은 메서드가 무엇을 하는지(WHAT)를 설명하는 주석이다.
  `// private`은 접근 제어자를 설명하는 주석으로, 코드 자체(`private` 키워드)로 자명하다.
  두 주석 모두 WHY가 아닌 WHAT에 해당하며, 구현 시 그대로 코드에 작성될 위험이 있다.
  (반면 `// Router → MainView 시스템 현황 파라미터 제공`은 호출 이유(WHY)를 설명하므로 허용된다.)
- **권장 수정**:
  - `// FR-MON-01·02 단건 렌더링` 삭제 — 메서드명 `showMonitoring()`으로 자명
  - `// private` 삭제 — `private` 접근 제어자가 직접 표현하므로 불필요

---

### [WARNING] WHAT 주석 — `render()` 내부 호출 순서 기술 및 ANSI 상수 그룹 주석

- **위치**: `phase4.md` — Section 3.1 (ANSI 상수), Section 3.2 (MonitoringView 메서드 명세)
- **위반 규칙**: CLAUDE.md 코딩 컨벤션 — "주석: WHY가 비자명한 경우에만 한 줄 이내로 작성"
- **현재 설계**:
  ```java
  // 재고 상태 색상
  private static final String ANSI_RESET  = "\033[0m";
  private static final String ANSI_YELLOW = "\033[33m";
  private static final String ANSI_RED    = "\033[31m";

  // 주문 상태 색상
  private static final String ANSI_BLUE   = "\033[34m";
  ...

  void render(...)
      // clearScreen() → printHeader(timestamp) → printOrderSummary(statusCounts) → printInventory(sampleStatuses)
  ```
  `// 재고 상태 색상`, `// 주문 상태 색상`은 상수 그룹이 무엇인지(WHAT)를 설명하는 주석이다.
  상수명(`ANSI_YELLOW` = 부족, `ANSI_RED` = 고갈)으로 용도가 자명하다.
  `render()` 내부의 호출 순서 주석도 구현 흐름(WHAT)을 기술하는 것이다.
- **권장 수정**:
  - `// 재고 상태 색상`, `// 주문 상태 색상` 블록 주석 삭제
  - `render()` 내 `// clearScreen() → printHeader(...) → ...` 주석 삭제 — 코드 순서 자체로 자명

---

### [WARNING] 미사용 메서드 설계 포함 — `printShutdownMessage()`

- **위치**: `phase4.md` — Section 3.2 (MonitoringView 메서드 명세)
- **위반 규칙**: CLAUDE.md 아키텍처 원칙 및 불필요한 복잡성 금지 — "요구사항에 없는 기능 설계 포함 금지"
- **현재 설계**:
  ```java
  void printShutdownMessage()  // "모니터링을 종료합니다." — 하위호환용, Router에서는 호출 안 함
  ```
  설계 문서 스스로 "Router에서는 호출 안 함"이라고 명시하고 있다.
  PRD의 FR-MON-01·02 어디에도 종료 메시지 출력 요구사항이 없으며, 단일 콘솔 앱에서
  스레드 루프(`MonitoringLoop`)가 제거된 이상 이 메서드의 존재 이유가 없다.
  "하위호환용"이라는 이유는 현재 요구사항 범위 밖의 미래 사용을 위한 확장 포인트이며,
  오버엔지니어링에 해당한다. 테스트 계획(Section 6.1)에 `printShutdownMessage_텍스트_포함`
  테스트까지 포함되어 있어 불필요한 테스트 부담도 발생한다.
- **권장 수정**:
  - `printShutdownMessage()` 메서드를 설계에서 제거
  - Section 6.1의 `printShutdownMessage_텍스트_포함` 테스트 항목도 함께 제거
  - DataMonitor 이식 시 미사용 메서드는 이식 대상에서 명시적으로 제외

---

## 검증 결과 요약

- [A] 아키텍처 제약: 통과
- [B] 코딩 컨벤션: 위반 (WARNING 2건 — WHAT 주석)
- [C] 보안: 통과
- [D] 불필요한 복잡성: 위반 (WARNING 1건 — 미사용 메서드)

---

## 참고: 통과 항목 근거

| 검증 항목 | 결과 | 근거 |
|-----------|------|------|
| 클래스명 PascalCase | 통과 | `MonitoringController`, `MonitoringView`, `ReleaseController`, `ReleaseView`, `SpyMonitoringView`, `StubSampleRepository`, `StubOrderRepository` 모두 준수 |
| 메서드·변수명 camelCase | 통과 | `showMonitoring`, `getSampleSummary`, `calcStockLevel`, `processRelease`, `statusCounts`, `demandSum`, `capturedStatusCounts`, `renderCallCount` 등 모두 준수 |
| 상수 UPPER_SNAKE_CASE | 통과 | `ANSI_RESET`, `ANSI_YELLOW`, `ANSI_RED`, `ANSI_BLUE`, `ANSI_ORANGE`, `ANSI_GREEN`, `ANSI_MAGENTA` 모두 준수 |
| MVC 레이어 분리 | 통과 | Controller가 `System.out` 직접 출력 없음, View가 Model 직접 수정 없음 |
| 생성자 주입(Constructor Injection) | 통과 | `MonitoringController(SampleRepository, OrderRepository, MonitoringView)`, `ReleaseController(OrderRepository, ReleaseView, Scanner)` 모두 생성자 주입 |
| View static 출력 메서드 금지 | 통과 | 설계에 static 메서드 없음, 모두 인스턴스 메서드로 설계 |
| 인터페이스 I 접두사 금지 | 통과 | `SampleRepository`, `OrderRepository` — I 접두사 없음 |
| Repository 구현체 접두사 | 통과 | `JsonSampleRepository`, `JsonOrderRepository` 모두 `Json` 접두사 준수 |
| 패키지명 소문자 | 통과 | `org.ssemi`, `org.ssemi.fixture` 등 모두 소문자 |
| 수율 계산식 위치 | 통과 | Phase 4 설계에 수율 계산식 없음 (`ProductionLineController`는 Phase 4 범위 외) |
| Router 역할 준수 | 통과 | `getSampleSummary()` 호출이 Router의 Controller 위임 범주에 해당, 집계 연산은 Controller 내부에 위치 |
| 보안 — 입력 검증 위치 | 통과 | `NumberFormatException` 및 범위 초과 검증이 사용자 입력 경계(`processRelease()`)에서만 수행 |
| 보안 — OWASP Top 10 | 통과 | JSON 파일 기반으로 SQL 인젝션 위험 없음, 민감 정보 처리 없음, 내부 API 과잉 검증 없음 |
