# Phase 4 설계 — 모니터링 + 출고 처리 기능

**버전**: 1.0.0  
**작성일**: 2026-06-12  
**범위**: FR-MON-01·02, FR-REL-01  
**신규 파일**: `MonitoringController`, `MonitoringView`, `ReleaseController`, `ReleaseView`  
**선행 Phase**: Phase 1 (도메인 모델 레이어)

---

## 1. 개요

| 클래스 | 출처 | 변경 사항 |
|--------|------|---------|
| `MonitoringController` | DataMonitor 이식 | `refresh()` → `showMonitoring()`, `shutdown()` 제거, `getSampleSummary()` 추가, ID 타입 Long→String |
| `MonitoringView` | DataMonitor 이식 | 패키지 변경, `%4d sampleId` → `%-8s sampleId`, 주문 상태 ANSI 색상 추가(BLUE/ORANGE/GREEN/MAGENTA), `printShutdownMessage` 제거 |
| `ReleaseController` | 신규 | — |
| `ReleaseView` | 신규 | — |

---

## 2. MonitoringController

### 2.1 생성자

```java
MonitoringController(SampleRepository sampleRepo, OrderRepository orderRepo, MonitoringView view)
```

### 2.2 메서드

```java
void showMonitoring()
List<SampleStatus> getSampleSummary()
private String calcStockLevel(int stock, int demandSum)
```

### 2.3 showMonitoring() 구현

```
1. orderRepo.findAll() → orders
2. statusCounts: REJECTED 제외 후 상태별 count (Map<OrderStatus, Long>)
3. sampleRepo.findAll() → samples
4. 각 시료별 demandSum = orders 중 해당 sampleId && (RESERVED || PRODUCING) 수량 합산
5. SampleStatus(sampleId, name, stock, calcStockLevel) 리스트 구성
6. timestamp = LocalTime.now().format("HH:mm:ss")
7. view.render(statusCounts, sampleStatusList, timestamp)
```

### 2.4 getSampleSummary() 구현

```
1. orderRepo.findAll() → orders
2. sampleRepo.findAll() → samples
3. 각 시료별 demandSum 계산 (showMonitoring과 동일)
4. SampleStatus 리스트 반환 (view 호출 없음)
```

`getSampleSummary()`는 Router가 MainView 시스템 현황 집계를 위해 호출한다.  
view를 건드리지 않으므로 순수하게 데이터만 반환한다.

### 2.5 calcStockLevel(int stock, int demandSum)

```java
if (stock == 0)          return "고갈";
if (stock < demandSum)   return "부족";
return "여유";           // stock >= demandSum (demandSum == 0 포함)
```

---

## 3. MonitoringView

DataMonitor의 `MonitoringView`를 이식하되, `SampleStatus.sampleId`가 `String`으로 변경된 점과
단건 호출 방식에 맞게 조정한다.

### 3.1 ANSI 상수

```java
private static final String ANSI_RESET   = "\033[0m";
private static final String ANSI_YELLOW  = "\033[33m";
private static final String ANSI_RED     = "\033[31m";
private static final String ANSI_BLUE    = "\033[34m";
private static final String ANSI_ORANGE  = "\033[38;5;208m";
private static final String ANSI_GREEN   = "\033[32m";
private static final String ANSI_MAGENTA = "\033[35m";
```

### 3.2 메서드 명세

```java
void render(Map<OrderStatus, Long> statusCounts,
            List<SampleStatus> sampleStatuses,
            String timestamp)

void clearScreen()

void printHeader(String timestamp)
    // "=" 40개 구분선
    // "  S-Semi 생산주문관리 — 모니터링"
    // "  마지막 갱신: {timestamp}"
    // "=" 40개 구분선

void printOrderSummary(Map<OrderStatus, Long> statusCounts)
    // "[주문 현황]"
    // "  {ANSI_BLUE}RESERVED{RESET}   :  {N}건"
    // "  {ANSI_ORANGE}PRODUCING{RESET}  :  {N}건"
    // "  {ANSI_GREEN}CONFIRMED{RESET}  :  {N}건"
    // "  {ANSI_MAGENTA}RELEASE{RESET}    :  {N}건"

void printInventory(List<SampleStatus> sampleStatuses)
    // "[시료별 재고 현황]"
    // 헤더: "  ID       이름           재고      상태"
    // 구분선: "  -------- ------------ -------- ------"
    // 각 행: "  %-8s  %-12s  %7d  %s"  (sampleId, name, stock, colorize(stockLevel))
```

### 3.3 colorize(String stockLevel) (private)

```java
switch (stockLevel) {
    case "부족" → ANSI_YELLOW + "부족" + ANSI_RESET
    case "고갈" → ANSI_RED    + "고갈" + ANSI_RESET
    default     → stockLevel          // "여유"
}
```

---

## 4. ReleaseController

### 4.1 생성자

```java
ReleaseController(OrderRepository orderRepo, ReleaseView view, Scanner scanner)
```

`SampleRepository`는 주입받지 않는다. 출고 처리는 주문 상태 변경만 필요하며,
시료 재고는 변경하지 않는다.

### 4.2 processRelease() 흐름

```
1. orderRepo.findByStatus(CONFIRMED) → confirmedOrders
2. confirmedOrders.isEmpty() → view.printEmpty(); return
3. view.printOrderList(confirmedOrders)
4. view.printPrompt("출고할 주문 번호 > ")
5. 입력 파싱
   5a. NumberFormatException → view.printError("올바른 번호를 입력해 주세요."); return
   5b. 범위 초과 → view.printError("올바른 번호를 입력해 주세요."); return
6. 선택된 order.setStatus(RELEASE) → orderRepo.update(order)
7. view.printSuccess("출고 완료: {orderId}")
```

---

## 5. ReleaseView

### 5.1 메서드 명세

```java
void printPrompt(String prompt)          // System.out.print(prompt)

void printOrderList(List<Order> orders)
    // 헤더: "번호  주문번호              시료ID    고객명             수량     상태"
    // 구분선: "-" 72개
    // 각 행: "%-5d %-22s %-9s %-18s %-8s %s" (번호, orderId, sampleId, customerName, quantity+"ea", status)
    // 구분선: "-" 72개

void printSuccess(String message)        // "[성공] " + message
void printError(String message)          // "[오류] " + message
void printEmpty()                        // "출고 대기 주문이 없습니다."
```

---

## 6. 테스트 계획

### 6.1 MonitoringViewTest

`clearScreen()`의 ANSI 이스케이프가 캡처 출력을 오염하지 않도록,
`render()` 대신 `printHeader`, `printOrderSummary`, `printInventory`를 직접 호출한다.

테스트 픽스처:
```java
Map<OrderStatus, Long> statusCounts = Map.of(
    OrderStatus.RESERVED, 2L, OrderStatus.PRODUCING, 1L,
    OrderStatus.CONFIRMED, 3L, OrderStatus.RELEASE, 1L
);
List<SampleStatus> sampleStatuses = List.of(
    new SampleStatus("S-001", "실리콘 웨이퍼", 120, "여유"),
    new SampleStatus("S-002", "GaAs 기판",       0, "고갈"),
    new SampleStatus("S-003", "SiC 기판",        15, "부족")
);
```

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `printHeader_S_Semi_포함` | `"S-Semi"` 또는 `"모니터링"` 포함 |
| `printHeader_timestamp_포함` | 전달한 timestamp 문자열 포함 |
| `printHeader_구분선_포함` | `"========"` 포함 |
| `printOrderSummary_헤더_포함` | `"[주문 현황]"` 포함 |
| `printOrderSummary_RESERVED_건수_포함` | `"RESERVED"` + `"2"` 포함 |
| `printOrderSummary_RESERVED_ANSI_blue` | `"\033[34m"` + `"RESERVED"` 포함 |
| `printOrderSummary_PRODUCING_ANSI_orange` | `"\033[38;5;208m"` + `"PRODUCING"` 포함 |
| `printOrderSummary_CONFIRMED_ANSI_green` | `"\033[32m"` + `"CONFIRMED"` 포함 |
| `printOrderSummary_RELEASE_ANSI_magenta` | `"\033[35m"` + `"RELEASE"` 포함 |
| `printInventory_재고현황_헤더_포함` | `"[시료별 재고 현황]"` 포함 |
| `printInventory_여유_포함` | `"여유"` 포함 |
| `printInventory_부족_ANSI_yellow` | `"\033[33m"` + `"부족"` 포함 |
| `printInventory_고갈_ANSI_red` | `"\033[31m"` + `"고갈"` 포함 |

### 6.2 MonitoringControllerTest

**테스트 인프라**:
```java
// SpyMonitoringView: render() 호출을 캡처, clearScreen() 무효화
private static class SpyMonitoringView extends MonitoringView {
    Map<OrderStatus, Long> capturedStatusCounts;
    List<SampleStatus> capturedSampleStatuses;
    int renderCallCount = 0;

    @Override
    public void render(Map<OrderStatus, Long> statusCounts,
                       List<SampleStatus> sampleStatuses, String timestamp) {
        this.capturedStatusCounts   = statusCounts;
        this.capturedSampleStatuses = sampleStatuses;
        this.renderCallCount++;
    }
    @Override public void clearScreen() {}
}

// StubSampleRepository: String ID 기반
private static class StubSampleRepository implements SampleRepository {
    private final List<Sample> samples;
    StubSampleRepository(Sample... samples) { this.samples = List.of(samples); }
    @Override public List<Sample> findAll() { return samples; }
    @Override public Optional<Sample> findById(String id) { return Optional.empty(); }
    // 나머지 메서드는 UnsupportedOperationException 또는 빈 구현
}

// StubOrderRepository: String ID 기반
private static class StubOrderRepository implements OrderRepository {
    private final List<Order> orders;
    StubOrderRepository(Order... orders) { this.orders = List.of(orders); }
    @Override public List<Order> findAll() { return orders; }
    @Override public List<Order> findByStatus(OrderStatus s) {
        return orders.stream().filter(o -> o.getStatus() == s).collect(Collectors.toList());
    }
    @Override public List<Order> findBySampleId(String id) { return List.of(); }
}
```

`calcStockLevel`은 `private` 메서드이므로 직접 호출 불가.
각 케이스는 StubRepository로 조건을 구성한 뒤 `showMonitoring()`을 호출하고,
`SpyMonitoringView.capturedSampleStatuses[i].stockLevel()`로 결과를 검증한다.

`getSampleSummary_*` 케이스는 `@TempDir` 기반 실제 JsonRepository를 사용한다.

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `showMonitoring_render_1회_호출` | `renderCallCount == 1` |
| `showMonitoring_REJECTED_제외_statusCounts` | `capturedStatusCounts`에 REJECTED 키 없음 |
| `showMonitoring_RESERVED_건수` | StubOrder: RESERVED 2건 → `capturedStatusCounts.get(RESERVED) == 2L` |
| `showMonitoring_CONFIRMED_제외_demandSum` | StubOrder: CONFIRMED 주문만 있는 시료 → stockLevel == "여유" |
| `showMonitoring_RELEASE_제외_demandSum` | StubOrder: RELEASE 주문만 있는 시료 → stockLevel == "여유" |
| `calcStockLevel_stock_0_고갈` | StubSample: stock=0, StubOrder: 없음 → stockLevel == "고갈" |
| `calcStockLevel_stock_lt_demand_부족` | StubSample: stock=5, StubOrder: RESERVED qty=10 → stockLevel == "부족" |
| `calcStockLevel_stock_eq_demand_여유` | StubSample: stock=50, StubOrder: RESERVED qty=50 → stockLevel == "여유" |
| `calcStockLevel_stock_gt_demand_여유` | StubSample: stock=100, StubOrder: RESERVED qty=50 → stockLevel == "여유" |
| `calcStockLevel_demand_0_여유` | StubSample: stock=10, StubOrder: CONFIRMED qty=100 → stockLevel == "여유" |
| `getSampleSummary_시료수_일치` | JsonSampleRepository에 2종 저장 후 `size() == 2` |
| `getSampleSummary_render_미호출` | `renderCallCount == 0` |

### 6.3 ReleaseViewTest

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `printOrderList_헤더_포함` | `"주문번호"` 포함 |
| `printOrderList_구분선_포함` | `"---"` 포함 |
| `printOrderList_orderId_포함` | 전달한 orderId 문자열 포함 |
| `printOrderList_빈_리스트_예외_없음` | `assertDoesNotThrow` |
| `printEmpty_메시지_포함` | `"없습니다"` 포함 |
| `printSuccess_성공_태그_포함` | `"[성공]"` 포함 |
| `printError_오류_태그_포함` | `"[오류]"` 포함 |

### 6.4 ReleaseControllerTest

**테스트 인프라**: 3개 repository는 동일 `@TempDir` 공유.

```java
@TempDir Path tempDir;
JsonOrderRepository   orderRepo;
ReleaseView           realView;
ByteArrayOutputStream outContent;

@BeforeEach
void setUp() {
    orderRepo  = new JsonOrderRepository(tempDir.resolve("orders.json"));
    outContent = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    realView   = new ReleaseView();
    // scanner는 각 테스트에서 new Scanner(new StringReader("입력값"))으로 구성
}
```

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `processRelease_CONFIRMED_없음_printEmpty` | 빈 DB → `"없습니다"` 출력 |
| `processRelease_정상_출고_RELEASE_전이` | CONFIRMED 주문 저장 → 입력 "1" → status == RELEASE |
| `processRelease_정상_출고_성공_메시지` | `"[성공]"` 포함 |
| `processRelease_잘못된_번호_오류_메시지` | 입력 "abc" → `"[오류]"` 포함 |
| `processRelease_범위_초과_번호_오류_메시지` | CONFIRMED 1건, 입력 "99" → `"[오류]"` 포함 |

출고 후 재고 무변경 검증은 Phase 5 `OrderFlowIntegrationTest` 시나리오 1·2에서 커버한다.
(`ReleaseController`에 `SampleRepository`가 없으므로 단위 테스트에서 재고 접근 자체가 불가)

---

## 7. 구현 주의사항

### 7.1 MonitoringView.printInventory String ID 출력

원본 DataMonitor는 `%4d`로 `long sampleId`를 출력한다.
이식 시 `%-8s`로 `String sampleId`를 출력하도록 변경한다.

```java
// 변경 전 (DataMonitor)
System.out.printf("  %4d  %-12s  %7d  %s%n", s.sampleId(), ...);

// 변경 후 (SampleOrderSystem)
System.out.printf("  %-8s  %-12s  %7d  %s%n", s.sampleId(), ...);
```

### 7.2 MonitoringView.clearScreen() 테스트 격리

`clearScreen()`(`"\033[H\033[2J"`)은 ByteArrayOutputStream 캡처 출력을 오염시킨다.

- **MonitoringViewTest**: `render()` 대신 `printHeader`, `printOrderSummary`, `printInventory`를 직접 호출한다.
- **MonitoringControllerTest**: `SpyMonitoringView`에서 `clearScreen()`을 빈 구현으로 override한다.

### 7.3 ReleaseController — 재고 변경 없음

출고 처리는 주문 상태를 RELEASE로 변경하는 것뿐이다.
`SampleRepository`를 주입받지 않으며, 재고를 건드리지 않는다.

### 7.4 MonitoringController.getSampleSummary() — view 호출 없음

`getSampleSummary()`는 `view.render()`를 호출하지 않는다.
Router에서 시스템 현황 집계 전용으로 사용하기 때문이다.

---

## 8. 완료 기준

- `./gradlew test` 전체 통과
- FR-MON-01·02: REJECTED 제외 확인, 재고 상태 3종 모두 검증
- FR-REL-01: CONFIRMED → RELEASE 상태 전이 및 저장 확인
- `MonitoringController.getSampleSummary()` view 미호출 확인
