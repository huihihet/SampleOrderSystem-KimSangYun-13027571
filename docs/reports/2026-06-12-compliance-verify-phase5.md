# 컴플라이언스 검증 보고서

**일시**: 2026-06-12  
**검증 대상**: `docs/design/phase5.md`  
**결과**: ❌ 위반 3건 (CRITICAL: 1, WARNING: 2)

---

## 발견된 위반

### [CRITICAL] Router.run()에서 System.out 직접 출력

- **위치**: `docs/design/phase5.md` — 섹션 4.2 `run()` 코드 블록
- **위반 규칙**:
  - `과제/CLAUDE.md` 아키텍처 레이어 규칙: Controller 레이어에서 `System.out` 직접 출력 금지
  - `SampleOrderSystem/CLAUDE.md` 코딩 컨벤션: "Router.java는 메뉴 루프와 Controller 위임만 담당"
- **현재 설계**:
  ```java
  System.out.println("프로그램을 종료합니다.");
  ```
  Router의 `run()` 메서드 종료 직전에 종료 메시지를 `System.out`으로 직접 출력한다. View 레이어를 우회하는 출력이며, Router 역할 정의("메뉴 루프와 Controller 위임만 담당")도 벗어난다.
- **권장 수정**: `MainView`에 `printExitMessage()` 메서드를 추가하고, Router는 `mainView.printExitMessage()`를 호출하도록 변경한다. `MainView` 메서드 명세(섹션 3.1)에 해당 메서드를 추가해야 한다.

---

### [WARNING] Router.run()에 인라인 집계 로직 존재

- **위치**: `docs/design/phase5.md` — 섹션 4.2 `run()` 코드 블록
- **위반 규칙**:
  - `SampleOrderSystem/CLAUDE.md` 코딩 컨벤션: "Router.java는 메뉴 루프와 Controller 위임만 담당"
  - `과제/CLAUDE.md` 아키텍처 레이어 규칙: 도메인 로직은 Model 또는 Controller 레이어에 위치
- **현재 설계**:
  ```java
  List<SampleStatus> summary = monitoringController.getSampleSummary();
  int  totalSamples = summary.size();
  long totalStock   = summary.stream().mapToLong(SampleStatus::stock).sum();
  ```
  `summary.stream().mapToLong(...).sum()` 집계 연산이 Router 내부에 직접 구현되어 있다. Router가 단순 Controller 위임을 넘어 데이터 가공 책임을 갖게 된다. 동일한 문제가 이전 PLAN.md 컴플라이언스 보고서(`2026-06-12-compliance-verify.md`)에서도 WARNING으로 기록된 바 있다.
- **권장 수정**: `MonitoringController`에 `getTotalStock()` 메서드를 추가하여 집계 로직을 Controller 레이어에 위임한다. Router는 `monitoringController.getTotalStock()`을 호출하기만 한다. 또는 `MainMenuSummary` VO를 도입해 `MonitoringController`가 집계 결과를 단일 객체로 반환하도록 설계한다.

---

### [WARNING] 설계 코드 블록 내 WHAT 주석 다수 포함

- **위치**: `docs/design/phase5.md` — 섹션 2.1, 2.2, 2.3, 4.2 코드 블록
- **위반 규칙**:
  - `과제/CLAUDE.md` 코딩 컨벤션: "주석: WHY가 비자명한 경우에만 한 줄 이내로 작성"
  - `SampleOrderSystem/CLAUDE.md` 동일 상속
- **현재 설계**: 다음 주석들은 코드가 무엇을 하는지(WHAT)를 설명하며, 메서드명·코드 자체만으로 이미 자명하다.
  - `view.printMenu(); // [1] 등록  [2] 목록  [3] 검색  [0] 뒤로` (섹션 2.1)
  - `int choice = readMenuChoice(); // 파싱 실패 → printError + continue` (섹션 2.1)
  - `view.printApprovalMenu(); // [1] 주문 승인  [2] 주문 거절  [0] 뒤로` (섹션 2.2)
  - `view.printMenu(); // [1] 현황 조회  [2] 생산 완료  [0] 뒤로` (섹션 2.3)
  - `// 시스템 현황 집계` (섹션 4.2)
- **권장 수정**: 위 주석을 모두 제거한다. 메뉴 항목 정보는 View 구현에서 관리한다. `readMenuChoice()`의 동작을 설명해야 한다면 "NumberFormatException 흡수로 switch default 분기에서 일관 처리" 형태의 WHY 주석으로 재작성한다.

---

## 검증 결과 요약

- [A] 아키텍처 제약: ❌ (Router의 `System.out` 직접 출력 — CRITICAL, Router 집계 로직 내재화 — WARNING)
- [B] 코딩 컨벤션: ❌ (WHAT 주석 다수 — WARNING)
- [C] 보안: ✅
- [D] 불필요한 복잡성: ✅
