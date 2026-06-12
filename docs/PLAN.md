# PLAN — S-Semi 시료 생산주문관리 시스템

**문서 버전**: 1.1.0  
**최초 작성**: 2026-06-12  
**최종 수정**: 2026-06-12  
**참조 문서**: [PRD.md](PRD.md)

---

## 전체 Phase 구성

| Phase | 제목 | 주요 산출물 | 선행 Phase |
|-------|------|-----------|-----------|
| Phase 0 | 프로젝트 환경 설정 | `build.gradle`, 빈 JSON 파일 | 없음 |
| Phase 1 | 도메인 모델 레이어 | Entity 5종, Repository 6종, JsonFileUtil | Phase 0 |
| Phase 2 | 시료 관리 기능 | SampleController, SampleView | Phase 1 |
| Phase 3 | 주문 처리 + 생산 라인 기능 | OrderController, ProductionLineController, 관련 View | Phase 1 |
| Phase 4 | 모니터링 + 출고 처리 기능 | MonitoringController, MonitoringView, ReleaseController, ReleaseView | Phase 1 |
| Phase 5 | 앱 조립 + 통합 테스트 | Router, MainView, Main, 테스트 픽스처, 통합 테스트 | Phase 2~4 |

---

## Phase 0: 프로젝트 환경 설정

### 목표

빌드·실행·테스트 환경을 완성한다.

### 작업 목록

#### 0-1. `build.gradle` 업데이트

```groovy
plugins {
    id 'java'
    id 'application'
    id 'jacoco'
}

application {
    mainClass = 'org.ssemi.Main'
}

dependencies {
    implementation 'com.google.code.gson:gson:2.11.0'

    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

jacocoTestReport {
    dependsOn test
    reports { xml.required = true; html.required = true }
}
```

#### 0-2. 데이터 디렉토리 및 빈 JSON 파일 생성

```
src/main/resources/data/samples.json        → []
src/main/resources/data/orders.json         → []
src/main/resources/data/production_queue.json → []
```

#### 0-3. 테스트 리소스 디렉토리 생성

```
src/test/resources/data/   (비어있는 디렉토리)
```

### 완료 기준

- `./gradlew build` 성공 (소스 없어도 통과)
- `./gradlew dependencies` 에서 Gson 2.11.0 확인

---

## Phase 1: 도메인 모델 레이어

### 목표

모든 Entity, Repository 인터페이스, JSON 구현체를 완성한다.
이후 Phase들의 기반이 되며, **이 Phase 완료 전 다른 Phase는 시작하지 않는다**.

### 출처 요약

| 클래스 | 출처 | 변경 사항 |
|--------|------|---------|
| `Sample`, `Order`, `OrderStatus` | DataPersistence 이식 | 패키지 변경 |
| `SampleStatus` | DataMonitor 이식 | `long sampleId` → `String sampleId` |
| `ProductionQueueItem` | 신규 | — |
| `JsonFileUtil` | DataPersistence 이식 | 패키지 변경 |
| `SampleRepository` (interface) | DataPersistence 이식 | `findByNameContaining` 추가 |
| `OrderRepository` (interface) | DataPersistence 이식 | `findBySampleId` 추가, `deleteById` 제거 |
| `ProductionQueueRepository` (interface) | 신규 | — |
| `JsonSampleRepository` | DataPersistence 이식 | `findByNameContaining` 구현 추가 |
| `JsonOrderRepository` | DataPersistence 이식 | `findBySampleId` 구현 추가 |
| `JsonProductionQueueRepository` | 신규 | — |

### 신규 파일 목록

```
src/main/java/org/ssemi/
├── model/entity/
│   ├── Sample.java
│   ├── Order.java
│   ├── OrderStatus.java
│   ├── SampleStatus.java
│   └── ProductionQueueItem.java
└── model/repository/
    ├── SampleRepository.java
    ├── OrderRepository.java
    ├── ProductionQueueRepository.java
    ├── JsonFileUtil.java
    ├── JsonSampleRepository.java
    ├── JsonOrderRepository.java
    └── JsonProductionQueueRepository.java
```

### 설계 상세

#### Entity: `Sample`

DataPersistence의 `Sample`을 패키지만 변경해 이식한다.

```java
// no-arg 생성자 + 전체 인자 생성자 + Getter/Setter
// equals/hashCode: sampleId 기반
String sampleId, String name, int avgProductionTime, double yield, int stock

// 도메인 메서드 추가 (stock 차감 로직을 Controller 밖으로 격리)
void deductStock(int quantity)   // this.stock -= quantity; if (stock < 0) throw IllegalStateException
void addStock(int quantity)      // this.stock += quantity
```

#### Entity: `Order`

DataPersistence의 `Order`를 패키지만 변경해 이식한다.

```java
// no-arg 생성자 + 전체 인자 생성자 + Getter/Setter
// equals/hashCode: orderId 기반
String orderId, String sampleId, String customerName, int quantity, OrderStatus status

// 도메인 메서드 추가 (없음 — 상태 변경은 setStatus() 사용)
```

#### Entity: `OrderStatus`

DataPersistence의 `OrderStatus`를 패키지만 변경해 이식한다.

```
RESERVED, PRODUCING, CONFIRMED, RELEASE, REJECTED
```

#### Entity: `SampleStatus` (record)

DataMonitor의 `SampleStatus`를 이식하되, ID 타입을 변경한다.

```java
record SampleStatus(String sampleId, String name, int stock, String stockLevel) {}
// long sampleId → String sampleId
```

#### Entity: `ProductionQueueItem` (신규)

```java
// no-arg 생성자 + 전체 인자 생성자 + Getter/Setter
String queueId       // "Q-001" 포맷 (PRD Section 8.1 ID 형식 준수)
String orderId
String sampleId
int requiredQuantity        // 부족 수량 (주문 수량 - 재고, 항상 양수)
int actualProductionQuantity // ceil(requiredQuantity / (yield × 0.9))
int totalProductionTime      // avgProductionTime × actualProductionQuantity
String enqueuedAt            // LocalDateTime.now().toString()
```

#### Repository: `SampleRepository`

```java
void save(Sample sample);
Optional<Sample> findById(String sampleId);
List<Sample> findAll();
List<Sample> findByNameContaining(String keyword);
void update(Sample sample);
void deleteById(String sampleId);
```

#### Repository: `OrderRepository`

```java
void save(Order order);
Optional<Order> findById(String orderId);
List<Order> findAll();
List<Order> findByStatus(OrderStatus status);
List<Order> findBySampleId(String sampleId);
void update(Order order);
```

#### Repository: `ProductionQueueRepository`

```java
void enqueue(ProductionQueueItem item);
List<ProductionQueueItem> findAll();
Optional<ProductionQueueItem> findById(String queueId);
void deleteById(String queueId);
```

#### `JsonFileUtil`

DataPersistence의 `JsonFileUtil`을 그대로 이식한다. (변경 없음)

#### `JsonSampleRepository`

DataPersistence의 `JsonSampleRepository`를 이식하고 아래를 추가한다.

```java
// findByNameContaining 구현 추가
public List<Sample> findByNameContaining(String keyword) {
    if (keyword == null || keyword.isBlank()) return findAll();
    return findAll().stream()
        .filter(s -> s.getName().contains(keyword))
        .collect(Collectors.toList());
}
```

#### `JsonOrderRepository`

DataPersistence의 `JsonOrderRepository`를 이식하고 아래를 추가한다.

```java
// findBySampleId 구현 추가 (null 검증은 호출자 책임 — 내부 API)
public List<Order> findBySampleId(String sampleId) {
    return findAll().stream()
        .filter(o -> sampleId.equals(o.getSampleId()))
        .collect(Collectors.toList());
}
// deleteById 미구현 (인터페이스에 없음)
```

#### `JsonProductionQueueRepository` (신규)

`JsonFileUtil` 재사용. `JsonOrderRepository`와 동일한 구조로 작성한다.

```java
// TypeToken<List<ProductionQueueItem>>
// enqueue: 중복 queueId 검사 → save
// findAll, findById, deleteById
```

### 테스트 계획

| 테스트 파일 | 검증 대상 | 주요 케이스 |
|-----------|---------|-----------|
| `SampleTest` | Entity 생성·setter | 정상 생성, equals/hashCode |
| `OrderTest` | Entity 생성·setter | 정상 생성, OrderStatus 필드 |
| `ProductionQueueItemTest` | Entity 생성·setter | 정상 생성, 수치 필드 |
| `SampleStatusTest` | record | sampleId String 타입 확인 |
| `JsonFileUtilTest` | JSON I/O | DataPersistence 테스트 그대로 이식 |
| `JsonSampleRepositoryTest` | CRUD + findByNameContaining | DataPersistence 테스트 이식 + 검색 케이스 추가 |
| `JsonOrderRepositoryTest` | CRUD + findBySampleId | DataPersistence 테스트 이식 + findBySampleId 케이스 추가 |
| `JsonProductionQueueRepositoryTest` | enqueue / findAll / findById / deleteById | 빈 큐, 순서 유지(FIFO), 없는 ID 예외 |

### 완료 기준

- `./gradlew test` 전체 통과
- 모든 Repository 구현체가 `@TempDir` 기반 독립 테스트로 검증됨

---

## Phase 2: 시료 관리 기능

### 목표

FR-SAM-01(등록), FR-SAM-02(목록 조회), FR-SAM-03(이름 검색)을 구현한다.

### 출처 요약

| 클래스 | 출처 | 변경 사항 |
|--------|------|---------|
| `SampleController` | ConsoleMVC 이식 | String ID, JsonSampleRepository, update/delete 제거 |
| `SampleView` | ConsoleMVC 이식 | 메뉴 항목 조정(등록·목록·검색만 유지) |

### 신규 파일 목록

```
src/main/java/org/ssemi/
├── controller/SampleController.java
└── view/SampleView.java
```

### 설계 상세

#### `SampleController`

ConsoleMVC의 `SampleController`에서 `update()`, `delete()`, `findById()`를 제거하고
`String` ID 및 `JsonSampleRepository`로 교체한다.

```java
// 생성자 주입
SampleController(SampleRepository repository, SampleView view, Scanner scanner)

// 메서드
void register()       // sampleId·name·avgProductionTime·yield·stock 입력, 중복 ID 오류 처리
void listAll()        // 전체 목록 테이블 출력
void searchByName()   // 키워드 입력, findByNameContaining 호출
```

ID 자동 생성 방식: 사용자가 `sampleId`를 직접 입력 (FR-SAM-01 명세). 입력 예: `S-001`.

출력 포맷 예시:
```
S-001   실리콘 웨이퍼-8인치   0.5 min/ea   0.92   480 ea
```

#### `SampleView`

ConsoleMVC의 `SampleView`에서 update/delete 관련 메뉴·메서드를 제거한다.

```java
void printMenu()                            // [1] 시료 등록  [2] 목록 조회  [3] 이름 검색  [0] 뒤로
void printPrompt(String prompt)             // "선택 > " 프롬프트
void printSampleList(List<Sample> samples)
    // 컬럼: ID | 시료명 | 평균 생산시간 | 수율 | 현재 재고
    // 예: "S-001  실리콘 웨이퍼-8인치  0.5 min/ea  0.92  480 ea"
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

### 테스트 계획

| 테스트 파일 | 검증 대상 | 주요 케이스 |
|-----------|---------|-----------|
| `SampleViewTest` | 출력 포맷 | printMenu 포함 항목, 테이블 포맷, 성공/오류 메시지 |
| `SampleControllerTest` | register / listAll / searchByName | 정상 흐름, 중복 ID 오류, 파싱 실패, 빈 목록 |

**테스트 방식**: ConsoleMVC 패턴 재사용
- View: `ByteArrayOutputStream`으로 `System.out` 캡처
- Controller: 실제 `SampleView` + 실제 `JsonSampleRepository` (`@TempDir`) + `ByteArrayOutputStream` 캡처

### 완료 기준

- FR-SAM-01·02·03 시나리오 Controller 테스트 통과
- 잘못된 입력(비어있는 이름, 파싱 실패) 오류 처리 확인

---

## Phase 3: 주문 처리 + 생산 라인 기능

### 목표

FR-ORD-01(접수), FR-ORD-02(승인), FR-ORD-03(거절), FR-PROD-01(현황 조회), FR-PROD-02(생산 완료)를 구현한다.

두 기능을 한 Phase로 묶는 이유: 승인 시 `OrderController`가 `ProductionLineController`를 호출하여 생산 큐에 등록하는 **의존 관계**가 있다.

### 신규 파일 목록

```
src/main/java/org/ssemi/
├── controller/
│   ├── OrderController.java
│   └── ProductionLineController.java
└── view/
    ├── OrderView.java
    └── ProductionLineView.java
```

### 설계 상세

#### `ProductionLineController`

수율 계산 로직과 생산 큐 관리를 전담한다.

```java
// 생성자 주입
ProductionLineController(
    SampleRepository sampleRepo,
    OrderRepository orderRepo,
    ProductionQueueRepository queueRepo,
    ProductionLineView view,
    Scanner scanner
)

// 메서드
void showQueue()           // 생산 큐 전체 목록 출력 (FIFO 순번 포함)
void completeProduction()  // 큐 항목 선택 → 재고 증가 → 주문 CONFIRMED → 큐 항목 삭제

// OrderController에서 재고 부족 승인 시 호출
void registerProductionQueue(Order order, Sample sample)
    // requiredQuantity = order.getQuantity() - sample.getStock()
    // actualProductionQuantity = (int) Math.ceil(requiredQuantity / (sample.getYield() * 0.9))
    // totalProductionTime = sample.getAvgProductionTime() * actualProductionQuantity
    // queueId = "Q-" + String.format("%03d", ...)
    // enqueuedAt = LocalDateTime.now().toString()
```

#### `OrderController`

주문 접수·승인·거절 처리. 승인 시 `ProductionLineController`에 큐 등록을 위임한다.

```java
// 생성자 주입
OrderController(
    SampleRepository sampleRepo,
    OrderRepository orderRepo,
    ProductionLineController productionLineController,
    OrderView view,
    Scanner scanner
)

// 메서드
void placeOrder()    // sampleId 존재 확인 → Order 생성(RESERVED) → 저장
void approveOrder()  // RESERVED 목록 표시 → 선택 → 재고 비교
                     //   재고 충분: sample.deductStock(order.getQuantity()) → 상태 CONFIRMED
                     //   재고 부족: 상태 PRODUCING, productionLineController.registerProductionQueue() 호출
void rejectOrder()   // RESERVED 목록 표시 → 선택 → 상태 REJECTED
```

orderId 자동 생성: `"ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%04d", findAll().size() + 1)` (예: `ORD-20250416-0001`).

#### `OrderView`

```java
void printApprovalMenu()                      // [1] 주문 승인  [2] 주문 거절  [0] 뒤로 (route(3) 서브메뉴용)
void printPrompt(String prompt)               // "선택 > " 프롬프트
void printOrderList(List<Order> orders, Map<String, String> sampleNames)  // 번호·orderId·시료명·고객명·수량(ea)·상태(ANSI 색상) 테이블
void printApprovalDetail(Sample sample, Order order, int requiredQty, int actualProdQty, int prodTime)
    // "시료 {name} 현재 재고 {stock} ea  주문 수량 {quantity} ea  부족분 {requiredQty} ea"
    // "실생산량 {actualProdQty} ea / {prodTime} min"
    // "상태 변경  RESERVED → PRODUCING" 또는 "상태 변경  RESERVED → CONFIRMED"
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

#### `ProductionLineView`

```java
void printMenu()                                          // [1] 현황 조회  [2] 생산 완료  [0] 뒤로
void printPrompt(String prompt)
void printQueueList(List<ProductionQueueItem> items,
                    Map<String, String> sampleNames)      // 순서·주문번호·시료명·주문량(ea)·부족분(ea)·실생산량(ea)·예상완료 테이블
    // 테이블 하단 주석: "* 부족분 = 주문량 - 재고, 실생산량 = ceil(부족분 / (수율 * 0.9))"
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

### 테스트 계획

| 테스트 파일 | 검증 대상 | 주요 케이스 |
|-----------|---------|-----------|
| `OrderViewTest` | 출력 포맷 | 주문 목록 테이블, 성공/오류 메시지 |
| `ProductionLineViewTest` | 출력 포맷 | 큐 목록 테이블 |
| `OrderControllerTest` | placeOrder / approveOrder / rejectOrder | 재고 충분 CONFIRMED 전이, 재고 부족 PRODUCING+큐 등록, 거절 REJECTED |
| `ProductionLineControllerTest` | registerProductionQueue / completeProduction | 수율 계산 경계값, 완료 후 재고 증가·상태 전이·큐 삭제 |

**수율 계산 경계값 테스트 케이스**:

| 시나리오 | 부족분 | yield | 안전마진(×0.9) | 실 생산량 |
|---------|-------|-------|--------------|---------|
| 정수 나눔 | 10 | 1.0 | 0.9 | `ceil(10/0.9)` = 12 |
| 낮은 수율 | 10 | 0.5 | 0.45 | `ceil(10/0.45)` = 23 |
| 나눔 딱 떨어짐 | 9 | 1.0 | 0.9 | `ceil(9/0.9)` = 10 |

### 완료 기준

- FR-ORD-01·02·03, FR-PROD-01·02 시나리오 Controller 테스트 통과
- 수율 계산 경계값 3케이스 이상 검증
- 재고 충분/부족 두 경로 모두 커버

---

## Phase 4: 모니터링 + 출고 처리 기능

### 목표

FR-MON-01(상태별 주문 현황), FR-MON-02(시료별 재고 현황), FR-REL-01(출고 실행)을 구현한다.

### 출처 요약

| 클래스 | 출처 | 변경 사항 |
|--------|------|---------|
| `MonitoringController` | DataMonitor 이식 | String ID, `InMemoryRepository` → JSON repo, 스레드 루프 제거 |
| `MonitoringView` | DataMonitor 이식 | 패키지 변경, String sampleId 출력 포맷 수정(`%4d`→`%-8s`), 주문 상태 ANSI 색상 추가, `clearScreen()` 선택 적용 |
| `ReleaseController` | 신규 | — |
| `ReleaseView` | 신규 | — |

### 신규 파일 목록

```
src/main/java/org/ssemi/
├── controller/
│   ├── MonitoringController.java
│   └── ReleaseController.java
└── view/
    ├── MonitoringView.java
    └── ReleaseView.java
```

### 설계 상세

#### `MonitoringController`

DataMonitor의 `MonitoringController.refresh()`를 이식하되 스레드를 제거한다.
`calcStockLevel` 로직은 그대로 유지한다.

```java
// 생성자 주입
MonitoringController(SampleRepository sampleRepo, OrderRepository orderRepo, MonitoringView view)

// 메서드
void showMonitoring()  // refresh() 로직 직접 실행 (단건 호출 방식)

// 패키지 공개 메서드 추가 (Router → MainView 시료 요약용)
List<SampleStatus> getSampleSummary()   // 전체 시료 목록을 SampleStatus로 변환해 반환

// private
String calcStockLevel(int stock, int demandSum)
    // stock == 0 → "고갈"
    // stock < demandSum → "부족"
    // stock >= demandSum → "여유"  (demandSum == 0 포함)
```

`demandSum`: 해당 시료의 RESERVED + PRODUCING 상태 주문 수량 합산 (CONFIRMED·RELEASE·REJECTED 제외)

#### `MonitoringView`

DataMonitor의 `MonitoringView`를 패키지만 변경해 이식한다. (ANSI 색상 포함)

- 상태별 주문 수: `RESERVED`(파란), `PRODUCING`(주황), `CONFIRMED`(초록), `RELEASE`(보라) 색상 적용
- 재고 상태 색상: 여유(초록), 부족(주황), 고갈(빨간)
- 메뉴 진입 시 단건 렌더링이므로 `clearScreen()` 호출은 선택적으로 적용한다.

#### `ReleaseController`

```java
// 생성자 주입
ReleaseController(OrderRepository orderRepo, ReleaseView view, Scanner scanner)

// 메서드
void processRelease()
    // CONFIRMED 상태 주문 목록 표시
    // 번호 입력 → 선택한 주문 상태 RELEASE로 변경 → update 저장
    // 출고 완료 메시지 출력
```

#### `ReleaseView`

```java
void printPrompt(String prompt)
void printOrderList(List<Order> orders)   // CONFIRMED 주문 목록 (orderId·시료·고객·수량)
void printSuccess(String message)
void printError(String message)
void printEmpty()                         // "출고 대기 주문이 없습니다."
```

### 테스트 계획

| 테스트 파일 | 검증 대상 | 주요 케이스 |
|-----------|---------|-----------|
| `MonitoringViewTest` | 출력 포맷 | DataMonitor 테스트 이식, ANSI 색상 코드 포함 |
| `MonitoringControllerTest` | showMonitoring | DataMonitor 테스트 이식 (String ID 적용), REJECTED 제외, calcStockLevel 경계값 |
| `ReleaseViewTest` | 출력 포맷 | CONFIRMED 목록, 빈 목록 메시지 |
| `ReleaseControllerTest` | processRelease | 정상 출고(RELEASE 전이), 빈 목록, 잘못된 번호 입력 |

**MonitoringController 재사용 테스트 패턴**:
- `SpyMonitoringView` (DataMonitor의 패턴 그대로 이식)
- `StubSampleRepository`, `StubOrderRepository` (String ID로 변경)
- `calcStockLevel` 경계값: `stock==demand → 여유`, `stock > demand → 여유`, `stock==0 → 고갈`, `stock < demand && stock > 0 → 부족`

### 완료 기준

- FR-MON-01·02, FR-REL-01 시나리오 Controller 테스트 통과
- MonitoringController: REJECTED 제외 확인, 재고 상태 3종 모두 검증
- ReleaseController: CONFIRMED → RELEASE 상태 전이 및 저장 확인

---

## Phase 5: 앱 조립 + 통합 테스트

### 목표

모든 컴포넌트를 조립하고 전체 시나리오를 검증한다.

### 기존 파일 변경 (메서드 추가)

| 클래스 | 추가 메서드 | 설명 |
|--------|-----------|------|
| `SampleController` | `handleSubMenu()` | 시료 서브메뉴 루프 |
| `OrderController` | `handleSubMenu()` | 주문 승인/거절 서브메뉴 루프 |
| `ProductionLineController` | `handleSubMenu()` | 생산라인 서브메뉴 루프 |
| `ProductionLineController` | `getQueueWaitingCount()` | 큐 대기 건수 (Router 현황 집계용) |
| `MonitoringController` | `getOrderCount()` | 전체 주문 건수 (Router 현황 집계용) |

### 신규 파일 목록

```
src/main/java/org/ssemi/
├── Main.java
├── view/MainView.java
└── app/Router.java

src/test/java/org/ssemi/
├── fixture/
│   ├── SampleFixture.java
│   └── OrderFixture.java
└── integration/
    └── OrderFlowIntegrationTest.java
```

### 설계 상세

#### `MainView`

PDF 화면 명세 기반 레이아웃 적용.

```java
void printMainMenu(int totalSamples, long totalStock, long totalOrders, long prodLineWaiting)
    // 구분선: 63개 '=' 문자
    // "반도체 시료 생산주문관리 시스템"
    // "시스템 현황  {YYYY-MM-DD HH:MM:SS}"
    // "등록 시료 |{totalSamples}종    출 재고    {totalStock,} ea"
    // "전체 주문  {totalOrders}건    생산라인   {prodLineWaiting}건 대기"
    // 구분선
    // "[1] 시료 관리          [2] 시료 주문"
    // "[3] 주문 승인/거절     [4] 모니터링"
    // "[5] 생산라인 조회      [6] 출고 처리"
    // "[0] 종료"
void printPrompt(String prompt)   // "선택 > " 프롬프트
void printError(String message)
```

#### `Router`

ConsoleMVC의 `Router`를 6개 메뉴로 확장한다.
시료 요약은 `MonitoringController.getSampleSummary()`에 위임한다 (`SampleRepository` 직접 접근 금지).

```java
// 생성자 주입
Router(
    SampleController sampleController,
    OrderController orderController,
    MonitoringController monitoringController,
    ReleaseController releaseController,
    ProductionLineController productionLineController,
    MainView mainView,
    Scanner scanner
)

// 메서드
void run()    // 메인 루프: getSampleSummary() 호출 → 메뉴 출력 → 입력 → 위임 반복
boolean route(int menu)
    // 1 → sampleController 서브메뉴 루프
    // 2 → orderController.placeOrder() (주문 접수 직접 실행)
    // 3 → orderController 서브메뉴 루프 (승인/거절: [1]승인 [2]거절 [0]뒤로)
    // 4 → monitoringController.showMonitoring()
    // 5 → productionLineController 서브메뉴 루프
    // 6 → releaseController.processRelease()
    // 0 → false (종료)
```

시료 요약: `monitoringController.getSampleSummary()` 호출 → 결과를 집계하여 `mainView.printMainMenu()` 파라미터로 전달.

#### `Main`

DI 조립 전용. 비즈니스 로직 없음.

```java
public static void main(String[] args) {
    // 파일 경로 설정
    Path samplesPath = Path.of("src/main/resources/data/samples.json");
    Path ordersPath  = Path.of("src/main/resources/data/orders.json");
    Path queuePath   = Path.of("src/main/resources/data/production_queue.json");

    Scanner scanner = new Scanner(System.in);

    // Repository 생성
    SampleRepository sampleRepo = new JsonSampleRepository(samplesPath);
    OrderRepository  orderRepo  = new JsonOrderRepository(ordersPath);
    ProductionQueueRepository queueRepo = new JsonProductionQueueRepository(queuePath);

    // View 생성
    MainView mainView = new MainView();
    SampleView sampleView = new SampleView();
    OrderView orderView = new OrderView();
    MonitoringView monitoringView = new MonitoringView();
    ReleaseView releaseView = new ReleaseView();
    ProductionLineView prodLineView = new ProductionLineView();

    // Controller 생성
    SampleController sampleCtrl = new SampleController(sampleRepo, sampleView, scanner);
    ProductionLineController prodLineCtrl = new ProductionLineController(sampleRepo, orderRepo, queueRepo, prodLineView, scanner);
    OrderController orderCtrl = new OrderController(sampleRepo, orderRepo, prodLineCtrl, orderView, scanner);
    MonitoringController monitorCtrl = new MonitoringController(sampleRepo, orderRepo, monitoringView);
    ReleaseController releaseCtrl = new ReleaseController(orderRepo, releaseView, scanner);

    // 앱 실행
    new Router(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodLineCtrl, mainView, scanner).run();
}
```

#### 테스트 픽스처: `SampleFixture`

DummyDataGenerator의 `SampleGenerator`를 간소화·이식한다.

```java
// String ID 기반, GeneratorConfig 없이 count·seed만 받음
List<Sample> generate(int count, long seed)
    // sampleId: "S-001", "S-002", ... (PRD Section 8.1 ID 형식 준수)
    // 나머지 필드: DummyDataGenerator 로직 그대로
```

#### 테스트 픽스처: `OrderFixture`

DummyDataGenerator의 `OrderGenerator`를 간소화·이식한다.

```java
List<Order> generate(List<String> sampleIds, int count, long seed)
    // orderId: "ORD-{YYYYMMDD}-{NNNN}" 형식 (PRD Section 8.1 ID 형식 준수)
    //          예: "ORD-20260101-0001"
    // 나머지 필드: DummyDataGenerator 로직 그대로
```

#### 통합 테스트: `OrderFlowIntegrationTest`

```
시나리오 1: 주문 접수 → 승인(재고 충분) → 출고
시나리오 2: 주문 접수 → 승인(재고 부족) → 생산 완료 → 출고
시나리오 3: 주문 접수 → 거절
시나리오 4: 모니터링 — REJECTED 제외 확인, 재고 상태별 표시
```

모든 시나리오는 `@TempDir` 기반 JSON 파일로 격리 실행한다.

### 테스트 계획

| 테스트 파일 | 검증 대상 | 주요 케이스 |
|-----------|---------|-----------|
| `SampleFixtureTest` | generate 결과 | ID 포맷, 개수 일치, 동일 seed 재현성 |
| `OrderFixtureTest` | generate 결과 | ID 포맷, sampleId 참조 무결성 |
| `RouterTest` | route 분기 | 6개 메뉴 각각 올바른 Controller 호출, menu=0 종료 |
| `MainViewTest` | 출력 포맷 | 시료 요약 포함, 6개 메뉴 항목 |
| `OrderFlowIntegrationTest` | 전체 시나리오 | 위 4개 시나리오 |

### 완료 기준

- `./gradlew test` 전체 통과
- `./gradlew jacocoTestReport` — Model·Controller 커버리지 80% 이상
- `./gradlew run` 실행 후 메인 메뉴 정상 출력 확인
- 통합 테스트 4개 시나리오 통과

---

## 전체 파일 생성 순서 요약

```
Phase 0  build.gradle, resources/data/*.json
Phase 1  model/entity/*.java, model/repository/*.java
Phase 2  controller/SampleController.java, view/SampleView.java
Phase 3  controller/OrderController.java, controller/ProductionLineController.java
         view/OrderView.java, view/ProductionLineView.java
Phase 4  controller/MonitoringController.java, controller/ReleaseController.java
         view/MonitoringView.java, view/ReleaseView.java
Phase 5  view/MainView.java, app/Router.java, Main.java
         test/.../fixture/SampleFixture.java, test/.../fixture/OrderFixture.java
         test/.../integration/OrderFlowIntegrationTest.java
```
