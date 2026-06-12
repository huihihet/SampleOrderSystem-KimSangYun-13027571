# Phase 5 설계 — 앱 조립 + 통합 테스트

**버전**: 1.0.0  
**작성일**: 2026-06-12  
**범위**: Main.java, MainView, Router, SampleFixture, OrderFixture, OrderFlowIntegrationTest  
**신규 파일**: 6개  
**기존 파일 변경**: SampleController, OrderController, ProductionLineController, MonitoringController에 메서드 추가  
**선행 Phase**: Phase 2·3·4 완료

---

## 1. 개요

| 파일 | 분류 | 설명 |
|------|------|------|
| `Main.java` | Entry point | 전체 DI 조립 + Router 실행 |
| `view/MainView.java` | 신규 View | 메인 화면 (PRD 8.2) |
| `app/Router.java` | 신규 Router | 메뉴 루프 + Controller 위임 (6메뉴) |
| `fixture/SampleFixture.java` | 신규 테스트 픽스처 | DummyDataGenerator 이식, String ID |
| `fixture/OrderFixture.java` | 신규 테스트 픽스처 | DummyDataGenerator 이식, String ID |
| `integration/OrderFlowIntegrationTest.java` | 신규 통합 테스트 | 4개 시나리오 |

### 기존 Controller 메서드 추가

| 클래스 | 추가 메서드 | 역할 |
|--------|-----------|------|
| `SampleController` | `handleSubMenu()` | 시료 서브메뉴 루프 |
| `OrderController` | `handleSubMenu()` | 주문 승인/거절 서브메뉴 루프 |
| `ProductionLineController` | `handleSubMenu()` | 생산라인 서브메뉴 루프 |
| `ProductionLineController` | `getQueueWaitingCount()` | 큐 대기 건수 반환 |
| `MonitoringController` | `getOrderCount()` | 전체 주문 건수 반환 |
| `MonitoringController` | `getSampleCount()` | 등록 시료 수 반환 |
| `MonitoringController` | `getTotalStock()` | 전체 재고 합산 반환 |

---

## 2. 기존 Controller 메서드 추가 명세

### 2.1 SampleController.handleSubMenu()

```java
public void handleSubMenu() {
    while (true) {
        view.printMenu();
        int choice = readMenuChoice();
        switch (choice) {
            case 1 -> register();
            case 2 -> listAll();
            case 3 -> searchByName();
            case 0 -> { return; }
            default -> view.printError("올바른 번호를 입력해 주세요.");
        }
    }
}
```

`readMenuChoice()`는 scanner.nextLine() + Integer.parseInt(); NumberFormatException 시 -1 반환.

### 2.2 OrderController.handleSubMenu()

```java
public void handleSubMenu() {
    while (true) {
        view.printApprovalMenu();
        int choice = readMenuChoice();
        switch (choice) {
            case 1 -> approveOrder();
            case 2 -> rejectOrder();
            case 0 -> { return; }
            default -> view.printError("올바른 번호를 입력해 주세요.");
        }
    }
}
```

### 2.3 ProductionLineController.handleSubMenu()

```java
public void handleSubMenu() {
    while (true) {
        view.printMenu();
        int choice = readMenuChoice();
        switch (choice) {
            case 1 -> showQueue();
            case 2 -> completeProduction();
            case 0 -> { return; }
            default -> view.printError("올바른 번호를 입력해 주세요.");
        }
    }
}
```

### 2.4 ProductionLineController.getQueueWaitingCount()

```java
public int getQueueWaitingCount() {
    return queueRepo.findAll().size();
}
```

### 2.5 MonitoringController — 현황 집계 메서드 추가

```java
public int getOrderCount() {
    return orderRepo.findAll().size();
}

public int getSampleCount() {
    return sampleRepo.findAll().size();
}

public long getTotalStock() {
    return sampleRepo.findAll().stream().mapToLong(Sample::getStock).sum();
}
```

---

## 3. MainView

PRD Section 8.2 레이아웃 기반. 구분선은 `=` 63개.

### 3.1 메서드 명세

```java
void printMainMenu(int totalSamples, long totalStock, long totalOrders, long prodLineWaiting)

void printPrompt(String prompt)    // "선택 > " 프롬프트

void printError(String message)    // "[오류] " + message

void printGoodbye()                // "프로그램을 종료합니다."
```

### 3.2 printMainMenu 출력 형식

```
===============================================================
반도체 시료 생산주문관리 시스템
시스템 현황  {YYYY-MM-DD HH:mm:ss}

등록 시료 |{totalSamples}종    출 재고    {totalStock,} ea
전체 주문  {totalOrders}건    생산라인   {prodLineWaiting}건 대기

[1] 시료 관리          [2] 시료 주문
[3] 주문 승인/거절     [4] 모니터링
[5] 생산라인 조회      [6] 출고 처리
[0] 종료
===============================================================
선택 > 
```

- 구분선: `"=".repeat(63)`
- 타임스탬프: `LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))`
- `totalStock` 천 단위 콤마: `String.format("%,d", totalStock)`

---

## 4. Router

### 4.1 생성자

```java
Router(
    SampleController sampleController,
    OrderController orderController,
    MonitoringController monitoringController,
    ReleaseController releaseController,
    ProductionLineController productionLineController,
    MainView mainView,
    Scanner scanner
)
```

### 4.2 run()

```java
public void run() {
    while (true) {
        int  totalSamples    = monitoringController.getSampleCount();
        long totalStock      = monitoringController.getTotalStock();
        long totalOrders     = monitoringController.getOrderCount();
        long prodLineWaiting = productionLineController.getQueueWaitingCount();

        mainView.printMainMenu(totalSamples, totalStock, totalOrders, prodLineWaiting);

        int choice = readMenuChoice();
        if (!route(choice)) break;
    }
    mainView.printGoodbye();
}
```

`readMenuChoice()` private: scanner.nextLine() + Integer.parseInt(); 실패 시 -1 반환.

### 4.3 route(int menu)

```java
public boolean route(int menu) {
    switch (menu) {
        case 1 -> sampleController.handleSubMenu();
        case 2 -> orderController.placeOrder();
        case 3 -> orderController.handleSubMenu();
        case 4 -> monitoringController.showMonitoring();
        case 5 -> productionLineController.handleSubMenu();
        case 6 -> releaseController.processRelease();
        case 0 -> { return false; }
        default -> mainView.printError("올바른 번호를 입력해 주세요.");
    }
    return true;
}
```

---

## 5. Main

DI 조립 전용. 비즈니스 로직 없음.

```java
public static void main(String[] args) {
    Path samplesPath = Path.of("src/main/resources/data/samples.json");
    Path ordersPath  = Path.of("src/main/resources/data/orders.json");
    Path queuePath   = Path.of("src/main/resources/data/production_queue.json");

    Scanner scanner = new Scanner(System.in);

    SampleRepository           sampleRepo = new JsonSampleRepository(samplesPath);
    OrderRepository            orderRepo  = new JsonOrderRepository(ordersPath);
    ProductionQueueRepository  queueRepo  = new JsonProductionQueueRepository(queuePath);

    MainView            mainView     = new MainView();
    SampleView          sampleView   = new SampleView();
    OrderView           orderView    = new OrderView();
    MonitoringView      monitorView  = new MonitoringView();
    ReleaseView         releaseView  = new ReleaseView();
    ProductionLineView  prodView     = new ProductionLineView();

    SampleController         sampleCtrl   = new SampleController(sampleRepo, sampleView, scanner);
    ProductionLineController prodLineCtrl = new ProductionLineController(sampleRepo, orderRepo, queueRepo, prodView, scanner);
    OrderController          orderCtrl    = new OrderController(sampleRepo, orderRepo, prodLineCtrl, orderView, scanner);
    MonitoringController     monitorCtrl  = new MonitoringController(sampleRepo, orderRepo, monitorView);
    ReleaseController        releaseCtrl  = new ReleaseController(orderRepo, releaseView, scanner);

    new Router(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodLineCtrl, mainView, scanner).run();
}
```

---

## 6. SampleFixture (test)

DummyDataGenerator의 `SampleGenerator`를 간소화 이식. `GeneratorConfig` 제거.

```java
package org.ssemi.fixture;

public class SampleFixture {
    private static final String[] MATERIALS =
        {"GaN", "SiC", "Si", "GaAs", "InP", "Ge", "SiGe", "AlGaN"};

    public static List<Sample> generate(int count, long seed) {
        Random random = new Random(seed);
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String material = MATERIALS[random.nextInt(MATERIALS.length)];
            String name     = material + "-" + String.format("%03d", random.nextInt(999) + 1);
            int    apt      = random.nextInt(451) + 30;
            double yield    = (random.nextInt(40) + 60) / 100.0;
            int    stock    = random.nextInt(501);
            samples.add(new Sample("S-" + String.format("%03d", i), name, apt, yield, stock));
        }
        return samples;
    }
}
```

---

## 7. OrderFixture (test)

DummyDataGenerator의 `OrderGenerator`를 간소화 이식. 상태는 전부 `RESERVED`.

```java
package org.ssemi.fixture;

public class OrderFixture {
    private static final String[] CUSTOMERS = {
        "Seoul Fab", "KAIST Lab", "SNU Research", "Yonsei Fab", "POSTECH Lab",
        "Korea Chip", "Nano Systems", "Alpha Fabless", "Beta Research", "Gamma Semi"
    };

    public static List<Order> generate(List<String> sampleIds, int count, long seed) {
        if (sampleIds == null || sampleIds.isEmpty())
            throw new IllegalArgumentException("sampleIds는 비어 있을 수 없습니다.");

        Random random = new Random(seed);
        List<Order> orders = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String orderId      = "ORD-20260101-" + String.format("%04d", i);
            String sampleId     = sampleIds.get(random.nextInt(sampleIds.size()));
            String customerName = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
            int    quantity     = random.nextInt(100) + 1;
            orders.add(new Order(orderId, sampleId, customerName, quantity, OrderStatus.RESERVED));
        }
        return orders;
    }
}
```

---

## 8. 테스트 계획

### 8.1 MainViewTest

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `printMainMenu_구분선_63개_포함` | `"=".repeat(63)` 포함 |
| `printMainMenu_시스템명_포함` | `"반도체 시료 생산주문관리"` 포함 |
| `printMainMenu_시스템현황_텍스트_포함` | `"시스템 현황"` 포함 |
| `printMainMenu_타임스탬프_패턴_포함` | 날짜 패턴 `\d{4}-\d{2}-\d{2}` 포함 |
| `printMainMenu_등록시료_수_포함` | `"12종"` 포함 (totalSamples=12) |
| `printMainMenu_출재고_포함` | `"2,540"` 포함 (totalStock=2540, 콤마 포맷) |
| `printMainMenu_전체주문_포함` | `"36건"` 포함 (totalOrders=36) |
| `printMainMenu_생산라인_포함` | `"32건 대기"` 포함 (prodLineWaiting=32) |
| `printMainMenu_6개_메뉴_모두_포함` | `"[1]"` ~ `"[6]"` + `"[0]"` 각각 포함 |
| `printMainMenu_선택_프롬프트_포함` | `"선택 >"` 포함 |
| `printError_오류_태그_포함` | `"[오류]"` 포함 |
| `printGoodbye_텍스트_포함` | `"종료"` 포함 |

### 8.2 RouterTest

RouterTest는 Mockito 없이 Spy 서브클래스 패턴을 사용한다.

**테스트 인프라**:

각 테스트 케이스에서 새 Spy 인스턴스를 생성하거나 `@BeforeEach`에서 재생성하여 플래그 초기화를 보장한다.
`@TempDir`은 `RouterTest` 클래스 필드로 선언하고, Spy 생성자에 `tempDir.resolve(...)` 경로를 명시적으로 전달한다.

```java
@TempDir Path tempDir;

private static class SpySampleController extends SampleController {
    boolean handleSubMenuCalled = false;
    SpySampleController(Path dir) {
        super(new JsonSampleRepository(dir.resolve("samples.json")),
              new SampleView(), new Scanner(new StringReader("")));
    }
    @Override public void handleSubMenu() { handleSubMenuCalled = true; }
}

private static class SpyOrderController extends OrderController {
    boolean placeOrderCalled    = false;
    boolean handleSubMenuCalled = false;
    SpyOrderController(Path dir, ProductionLineController plc) {
        super(new JsonSampleRepository(dir.resolve("samples.json")),
              new JsonOrderRepository(dir.resolve("orders.json")),
              plc, new OrderView(), new Scanner(new StringReader("")));
    }
    @Override public void placeOrder()    { placeOrderCalled    = true; }
    @Override public void handleSubMenu() { handleSubMenuCalled = true; }
}

// SpyMonitoringController, SpyReleaseController, SpyProductionLineController 동일 패턴
// 각 메서드(showMonitoring, processRelease, handleSubMenu)를 override해 flag 설정
```

각 테스트는 독립적인 Spy 인스턴스와 Router를 `@BeforeEach`에서 생성한다.
MainView는 `ByteArrayOutputStream` 캡처 버전을 사용한다.

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `route_1_sampleController_handleSubMenu_호출` | `SpySampleController.handleSubMenuCalled == true` |
| `route_2_orderController_placeOrder_호출` | `SpyOrderController.placeOrderCalled == true` |
| `route_3_orderController_handleSubMenu_호출` | `SpyOrderController.handleSubMenuCalled == true` |
| `route_4_monitoringController_showMonitoring_호출` | `SpyMonitoringController.showMonitoringCalled == true` |
| `route_5_productionLineController_handleSubMenu_호출` | `SpyProductionLineController.handleSubMenuCalled == true` |
| `route_6_releaseController_processRelease_호출` | `SpyReleaseController.processReleaseCalled == true` |
| `route_0_false_반환` | `assertFalse(router.route(0))` |
| `route_99_default_true_반환` | `assertTrue(router.route(99))` (오류 메시지 출력, 종료하지 않음) |

### 8.3 SampleFixtureTest

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `generate_개수_일치` | `generate(5, 42L).size() == 5` |
| `generate_ID_포맷_S_NNN` | 첫 번째 ID == `"S-001"`, 마지막 == `"S-005"` |
| `generate_동일_seed_재현성` | 같은 seed → 같은 name |
| `generate_yield_범위` | 모든 yield ∈ [0.60, 0.99] (`(nextInt(40)+60)/100.0` 최댓값 0.99) |

### 8.4 OrderFixtureTest

| 테스트 메서드 | 검증 내용 |
|-------------|---------|
| `generate_개수_일치` | `generate(sampleIds, 3, 42L).size() == 3` |
| `generate_ID_포맷_ORD` | 첫 번째 orderId == `"ORD-20260101-0001"` |
| `generate_sampleId_참조_무결성` | 모든 sampleId ∈ sampleIds |
| `generate_전체_RESERVED` | 모든 status == RESERVED |
| `generate_빈_sampleIds_IllegalArgumentException` | `assertThrows` |
| `generate_null_sampleIds_IllegalArgumentException` | `assertThrows` |

### 8.5 OrderFlowIntegrationTest

**테스트 인프라**:
```java
@TempDir Path tempDir;
// 3개 JSON 파일은 tempDir 하위에 생성
// 모든 Controller는 동일 Scanner + 동일 Repository 인스턴스 공유
```

Scanner 입력은 StringReader로 시나리오별 필요 입력을 순서대로 공급한다.

시나리오별 Scanner 입력 라인 구성 (각 컨트롤러 메서드 직접 호출, handleSubMenu 루프 경유 않음):

```
시나리오 1 전제: S-001 stock=200 사전 저장
  placeOrder(3라인):    "S-001\n고객A\n50\n"
  approveOrder(1라인):  "1\n"
  processRelease(1라인):"1\n"
  → 총 5라인: "S-001\n고객A\n50\n1\n1\n"

시나리오 2 전제: S-001 stock=5 사전 저장 (주문 수량 > 재고)
  placeOrder(3라인):           "S-001\n고객A\n50\n"
  approveOrder(1라인):         "1\n"   (PRODUCING 전이, 큐 자동 등록)
  completeProduction(1라인):   "1\n"   (생산 완료 → CONFIRMED)
  processRelease(1라인):       "1\n"   (출고 → RELEASE)
  → 총 6라인: "S-001\n고객A\n50\n1\n1\n1\n"

시나리오 3 전제: S-001 stock=100 사전 저장
  placeOrder(3라인):    "S-001\n고객A\n50\n"
  rejectOrder(1라인):   "1\n"
  → 총 4라인: "S-001\n고객A\n50\n1\n"

시나리오 4: 사전 저장 주문 3건(RESERVED 1, REJECTED 1, RELEASE 1), S-001 stock=0 저장
  showMonitoring(): Scanner 입력 불필요 (프롬프트 없음)
  → 0라인
```

| 시나리오 | 검증 내용 |
|---------|---------|
| **시나리오 1**: 접수 → 승인(재고 충분) → 출고 | 최종 status == RELEASE, `sample.stock == 200 - 50` 수치 검증 |
| **시나리오 2**: 접수 → 승인(재고 부족) → 생산 완료 → 출고 | PRODUCING 전이 확인, 큐 등록 확인, `completeProduction` 후 `sample.stock >= 주문수량` 수치 검증, 최종 status == RELEASE |
| **시나리오 3**: 접수 → 거절 | status == REJECTED, `sample.stock` 사전값과 동일 |
| **시나리오 4**: 모니터링 — REJECTED 제외, 재고 상태 표시 | `getSampleSummary()`의 REJECTED 주문 미포함 확인, stock=0 시료 stockLevel == "고갈" |

---

## 9. 구현 주의사항

### 9.1 handleSubMenu() — private readMenuChoice 공유

`readMenuChoice()`는 각 Controller 내부 private 헬퍼로 구현한다.
Scanner.nextLine() 파싱 실패 시 -1을 반환하고 switch default가 오류 처리한다.

```java
private int readMenuChoice() {
    try {
        return Integer.parseInt(scanner.nextLine().trim());
    } catch (NumberFormatException e) {
        return -1;
    }
}
```

### 9.2 Router — 동일 readMenuChoice 패턴

Router도 동일한 private readMenuChoice()를 구현한다.

### 9.3 통합 테스트 Scanner 입력 순서

각 컨트롤러 메서드가 소비하는 입력 라인 수:
- `SampleController.register()`: 5 라인 (sampleId, name, apt, yield, stock)
- `OrderController.placeOrder()`: 3 라인 (sampleId, customerName, quantity)
- `OrderController.approveOrder()`: 1 라인 (선택 번호)
- `OrderController.rejectOrder()`: 1 라인 (선택 번호)
- `ReleaseController.processRelease()`: 1 라인 (선택 번호)
- `ProductionLineController.completeProduction()`: 1 라인 (선택 번호)

StaticReader 기반 Scanner는 공급된 입력이 소진되면 `NoSuchElementException`을 발생시키므로,
시나리오별 입력을 정확히 계산하여 준비한다.

### 9.4 SampleFixture — yield 범위

DummyDataGenerator와 동일한 로직: `(random.nextInt(40) + 60) / 100.0` → 0.60 ~ 0.99 범위.
yield = 0.0 케이스는 픽스처에서 생성되지 않으므로 통합 테스트에서 직접 생성한다.

---

## 10. 완료 기준

- `./gradlew test` 전체 통과 (기존 162 + 신규 테스트)
- `./gradlew jacocoTestReport` — Model·Controller 커버리지 80% 이상
- `./gradlew run` 실행 → 메인 메뉴 정상 출력
- 통합 테스트 4개 시나리오 통과
