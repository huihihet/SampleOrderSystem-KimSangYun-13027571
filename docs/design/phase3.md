# Phase 3 설계 문서 — 주문 처리 + 생산 라인 기능

**문서 버전**: 1.0.0  
**작성일**: 2026-06-12  
**참조**: [PLAN.md](../PLAN.md) Phase 3, [PRD.md](../PRD.md) FR-ORD-01~03, FR-PROD-01~02  
**선행 Phase**: Phase 1, Phase 2 완료 필수

---

## 1. 목표

FR-ORD-01(주문 접수), FR-ORD-02(주문 승인), FR-ORD-03(주문 거절),  
FR-PROD-01(생산 현황 조회), FR-PROD-02(생산 완료 처리)를 구현한다.

두 기능을 한 Phase로 묶는 이유: 승인 시 `OrderController`가 `ProductionLineController`를 호출하여 생산 큐에 등록하는 **단방향 의존 관계**가 있다.

---

## 2. 산출물 목록

```
src/main/java/org/ssemi/
├── controller/
│   ├── OrderController.java
│   └── ProductionLineController.java
└── view/
    ├── OrderView.java
    └── ProductionLineView.java

src/test/java/org/ssemi/
├── controller/
│   ├── OrderControllerTest.java
│   └── ProductionLineControllerTest.java
└── view/
    ├── OrderViewTest.java
    └── ProductionLineViewTest.java
```

---

## 3. ANSI 색상 상수 (OrderView / ProductionLineView 공통)

PRD Section 8.6 기준. View 클래스 내에 private static final 상수로 정의한다.

```java
private static final String RESET   = "[0m";
private static final String BLUE    = "[34m";   // RESERVED
private static final String GREEN   = "[32m";   // CONFIRMED
private static final String ORANGE  = "[33m";   // PRODUCING (yellow로 대체)
private static final String RED     = "[31m";   // REJECTED
private static final String PURPLE  = "[35m";   // RELEASE
```

상태별 색상 매핑:

| OrderStatus | 색상 |
|-------------|------|
| RESERVED | BLUE |
| PRODUCING | ORANGE |
| CONFIRMED | GREEN |
| RELEASE | PURPLE |
| REJECTED | RED |

---

## 4. View 설계

### 4-1. `OrderView`

패키지: `org.ssemi.view`  
출처: 신규

**메서드 목록**:

```java
void printApprovalMenu()
void printPrompt(String prompt)
void printOrderList(List<Order> orders, Map<String, String> sampleNames)
void printApprovalDetail(Sample sample, Order order, int requiredQty, int actualProdQty, int prodTime)
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

#### `printApprovalMenu()`

```
[1] 주문 승인
[2] 주문 거절
[0] 뒤로
선택 > 
```

#### `printOrderList(List<Order> orders, Map<String, String> sampleNames)`

헤더 + 구분선(`-` 72개) + 행 반복:

```
번호  주문번호              시료명             고객명      수량    상태
------------------------------------------------------------------------
1     ORD-20260612-0001    실리콘 웨이퍼-8인치  홍길동      100 ea  RESERVED
2     ORD-20260612-0002    GaAs 기판            김철수       50 ea  PRODUCING
```

- 상태 컬럼에 ANSI 색상 적용
- `sampleNames`: `sampleId → 시료명` 맵 (Controller에서 조회 후 전달)

#### `printApprovalDetail(Sample sample, Order order, int requiredQty, int actualProdQty, int prodTime)`

재고 부족 경우:
```
시료 실리콘 웨이퍼-8인치  현재 재고 50 ea  주문 수량 100 ea  부족분 50 ea
실생산량 56 ea / 1680 min
상태 변경  RESERVED → PRODUCING
```

재고 충분 경우:
```
시료 실리콘 웨이퍼-8인치  현재 재고 200 ea  주문 수량 100 ea  부족분 0 ea
상태 변경  RESERVED → CONFIRMED
```

- `requiredQty == 0` 이면 부족분 행 없이 상태 변경만 출력
- `requiredQty > 0` 이면 실생산량·생산시간 행도 출력

#### `printEmpty()`

```
처리할 주문이 없습니다.
```

---

### 4-2. `ProductionLineView`

패키지: `org.ssemi.view`  
출처: 신규

**메서드 목록**:

```java
void printMenu()
void printPrompt(String prompt)
void printQueueList(List<ProductionQueueItem> items, Map<String, String> sampleNames)
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

#### `printMenu()`

```
[1] 현황 조회
[2] 생산 완료
[0] 뒤로
선택 > 
```

#### `printQueueList(List<ProductionQueueItem> items, Map<String, String> sampleNames)`

```
순서  주문번호              시료명              주문량   부족분   실생산량  예상완료(min)
-------------------------------------------------------------------------------------
1     ORD-20260612-0001    실리콘 웨이퍼-8인치   100 ea    50 ea    56 ea       1680
2     ORD-20260612-0002    GaAs 기판              50 ea    50 ea   112 ea       8064
-------------------------------------------------------------------------------------
* 부족분 = 주문량 - 재고, 실생산량 = ceil(부족분 / (수율 × 0.9))
```

- 구분선: `-` 85개
- 하단 주석 고정 출력

#### `printEmpty()`

```
생산 중인 항목이 없습니다.
```

---

## 5. Controller 설계

### 5-1. `ProductionLineController`

패키지: `org.ssemi.controller`  
출처: 신규. 수율 계산 로직 전담.

**생성자**:

```java
ProductionLineController(
    SampleRepository sampleRepo,
    OrderRepository orderRepo,
    ProductionQueueRepository queueRepo,
    ProductionLineView view,
    Scanner scanner
)
```

**메서드**:

#### `void showQueue()`

1. `queueRepo.findAll()` 호출
2. 비어있으면 `view.printEmpty()`
3. 아니면 각 항목의 `sampleId`로 `sampleRepo.findById()` 조회 → `Map<String, String> sampleNames` 구성
4. `view.printQueueList(items, sampleNames)`

#### `void completeProduction()`

1. `queueRepo.findAll()` 조회 → 비어있으면 `view.printEmpty()` 후 반환
2. `view.printQueueList()` 출력
3. `view.printPrompt("완료할 항목 번호 > ")` 후 번호 입력
4. 번호 파싱 실패 또는 범위 초과 → `view.printError("올바른 번호를 입력해 주세요.")` 후 반환
5. 선택된 `ProductionQueueItem` 조회
6. `sampleRepo.findById(item.getSampleId())` → 없으면 `view.printError()` 후 반환
7. `sample.addStock(item.getActualProductionQuantity())` → `sampleRepo.update(sample)`
8. `orderRepo.findById(item.getOrderId())` → `order.setStatus(OrderStatus.CONFIRMED)` → `orderRepo.update(order)`
9. `queueRepo.deleteById(item.getQueueId())`
10. `view.printSuccess("생산 완료: " + sampleName + " " + item.getActualProductionQuantity() + " ea → 재고 반영, 주문 CONFIRMED")`

#### `void registerProductionQueue(Order order, Sample sample)`

수율 계산 후 생산 큐 등록을 한 번에 처리. `OrderController`에서 호출.  
`createQueueItem`과 `enqueueItem`을 분리하면 thin wrapper가 생기므로 단일 메서드로 통합한다.

```java
int requiredQty = order.getQuantity() - sample.getStock();   // 항상 양수 (호출 전 검증 완료)
double effectiveYield = sample.getYield() * 0.9;
if (effectiveYield <= 0.0) {
    // yield가 0.0인 시료는 생산 불가 — 오류 처리 후 반환
    view.printError("수율이 0인 시료는 생산할 수 없습니다.");
    return;
}
int actualProdQty = (int) Math.ceil(requiredQty / effectiveYield);
int totalProdTime = sample.getAvgProductionTime() * actualProdQty;
String queueId = "Q-" + String.format("%03d", queueRepo.findAll().size() + 1);
String enqueuedAt = LocalDateTime.now().toString();
ProductionQueueItem item = new ProductionQueueItem(
    queueId, order.getOrderId(), order.getSampleId(),
    requiredQty, actualProdQty, totalProdTime, enqueuedAt);
queueRepo.enqueue(item);
```

---

### 5-2. `OrderController`

패키지: `org.ssemi.controller`  
출처: 신규. 주문 접수·승인·거절 전담.

**생성자**:

```java
OrderController(
    SampleRepository sampleRepo,
    OrderRepository orderRepo,
    ProductionLineController productionLineController,
    OrderView view,
    Scanner scanner
)
```

**메서드**:

#### `void placeOrder()`

1. `view.printPrompt("시료 ID > ")` 후 sampleId 입력
2. `sampleRepo.findById(sampleId)` → 없으면 `view.printError("존재하지 않는 시료 ID입니다.")` 후 반환
3. `view.printPrompt("고객명 > ")` 후 customerName 입력  
   → `customerName.isBlank()` 이면 `view.printError("고객명을 입력해 주세요.")` 후 반환
4. `view.printPrompt("주문 수량 > ")` 후 quantity 입력  
   → 파싱 실패: `view.printError("올바른 숫자를 입력해 주세요.")` 후 반환  
   → `quantity <= 0`: `view.printError("주문 수량은 1 이상이어야 합니다.")` 후 반환
5. `orderId` 자동 생성:
   ```java
   "ORD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
           + "-" + String.format("%04d", orderRepo.findAll().size() + 1)
   ```
6. `new Order(orderId, sampleId, customerName, quantity, OrderStatus.RESERVED)` 생성 후 `orderRepo.save()`
7. `view.printSuccess("주문이 접수되었습니다: " + orderId)`

#### `void approveOrder()`

1. `orderRepo.findByStatus(OrderStatus.RESERVED)` 조회  
   → 비어있으면 `view.printEmpty()` 후 반환
2. `sampleNames` 맵 구성 후 `view.printOrderList(reserved, sampleNames)`
3. `view.printPrompt("승인할 주문 번호 > ")` 후 번호 입력  
   → 파싱 실패 또는 범위 초과: `view.printError()` 후 반환
4. 선택된 `Order` 및 `Sample` 조회
5. **재고 충분** (`sample.getStock() >= order.getQuantity()`):
   - `sample.deductStock(order.getQuantity())` → `sampleRepo.update(sample)`
     (stock >= quantity 검증 후 호출이므로 IllegalStateException 발생 불가)
   - `order.setStatus(OrderStatus.CONFIRMED)` → `orderRepo.update(order)`
   - `view.printApprovalDetail(sample, order, 0, 0, 0)`
6. **재고 부족** (`sample.getStock() < order.getQuantity()`):
   - `order.setStatus(OrderStatus.PRODUCING)` → `orderRepo.update(order)`
   - `productionLineController.registerProductionQueue(order, sample)` 호출 (queueRepo 직접 접근 금지)
   - 표시용 수치는 동일 수식으로 계산:
     - `int requiredQty = order.getQuantity() - sample.getStock()`
     - `int actualProdQty = (int) Math.ceil(requiredQty / (sample.getYield() * 0.9))`
     - `int totalProdTime = sample.getAvgProductionTime() * actualProdQty`
   - `view.printApprovalDetail(sample, order, requiredQty, actualProdQty, totalProdTime)`

#### `void rejectOrder()`

1. `orderRepo.findByStatus(OrderStatus.RESERVED)` 조회  
   → 비어있으면 `view.printEmpty()` 후 반환
2. `view.printOrderList(reserved, sampleNames)` 출력
3. `view.printPrompt("거절할 주문 번호 > ")` 후 번호 입력  
   → 파싱 실패 또는 범위 초과: `view.printError()` 후 반환
4. `order.setStatus(OrderStatus.REJECTED)` → `orderRepo.update(order)`
5. `view.printSuccess("주문이 거절되었습니다: " + order.getOrderId())`

---

## 6. 테스트 계획

### 6-1. `OrderViewTest`

**테스트 방식**: `ByteArrayOutputStream`으로 `System.out` 캡처

| 케이스 | 검증 내용 |
|--------|---------|
| `printApprovalMenu()` | `[1]`, `[2]`, `[0]` 포함 확인 |
| `printOrderList(단일 항목)` | 헤더, 구분선, orderId·시료명·고객명·수량·상태 포함 |
| `printOrderList(복수 항목)` | 행 수 일치 확인 |
| `printOrderList(빈 리스트)` | 헤더·구분선 출력, 예외 없음 |
| `printApprovalDetail(재고 충분)` | `CONFIRMED` 포함, 실생산량 행 없음 |
| `printApprovalDetail(재고 부족)` | `PRODUCING` 포함, 실생산량·시간 포함 |
| `printEmpty()` | "없습니다" 포함 |
| `printSuccess(msg)` | `[성공]` 포함 |
| `printError(msg)` | `[오류]` 포함 |

### 6-2. `ProductionLineViewTest`

**테스트 방식**: `ByteArrayOutputStream` 캡처

| 케이스 | 검증 내용 |
|--------|---------|
| `printMenu()` | `[1]`, `[2]`, `[0]` 포함 |
| `printQueueList(단일 항목)` | 헤더, 구분선, 주문번호·시료명·수량 포함 |
| `printQueueList(복수 항목)` | 행 수 일치, 하단 주석 `*` 포함 |
| `printQueueList(빈 리스트)` | 헤더·구분선 출력, 예외 없음 |
| `printEmpty()` | "없습니다" 포함 |
| `printSuccess(msg)` | `[성공]` 포함 |
| `printError(msg)` | `[오류]` 포함 |

### 6-3. `ProductionLineControllerTest`

**테스트 방식**: 실제 Repository(`@TempDir`) + `ByteArrayOutputStream` 캡처

| 케이스 | 검증 내용 |
|--------|---------|
| `registerProductionQueue()` — 수율 1.0, 부족 10 | actualProdQty = `ceil(10/0.9)` = 12, 큐에 항목 추가 확인 |
| `registerProductionQueue()` — 수율 0.5, 부족 10 | actualProdQty = `ceil(10/0.45)` = 23 |
| `registerProductionQueue()` — 수율 1.0, 부족 9 | actualProdQty = `ceil(9/0.9)` = 10 (딱 떨어짐) |
| `registerProductionQueue()` — totalProductionTime | `avgProductionTime × actualProdQty` 확인 |
| `registerProductionQueue()` — queueId 포맷 | `"Q-001"` 형식 확인 |
| `registerProductionQueue()` — 수율 0.0 | `[오류]` 출력, 큐 항목 추가 안 됨 |
| `showQueue()` — 빈 큐 | "없습니다" 포함 출력 |
| `showQueue()` — 항목 있음 | 시료명 포함 출력 |
| `completeProduction()` — 정상 완료 | 재고 증가 확인, 주문 CONFIRMED 전이, 큐 항목 삭제 |
| `completeProduction()` — 빈 큐 | "없습니다" 출력 후 반환 |
| `completeProduction()` — 잘못된 번호 입력 | `[오류]` 출력, 큐·주문·재고 변경 없음 |
| `completeProduction()` — 범위 초과 번호 | `[오류]` 출력 |

**테스트 조립**: `sampleRepo`, `orderRepo`, `queueRepo` 각각 독립 `@TempDir` JSON 파일.  
`ProductionLineController(sampleRepo, orderRepo, queueRepo, new ProductionLineView(), scanner)` 직접 생성.

### 6-4. `OrderControllerTest`

**테스트 방식**: 실제 Repository(`@TempDir`) + `ByteArrayOutputStream` 캡처  
(Mockito 미사용)

**테스트 조립**:
```java
// 3개 Repository 공유 (동일 @TempDir)
ProductionLineController prodLineCtrl = new ProductionLineController(
    sampleRepo, orderRepo, queueRepo, new ProductionLineView(), new Scanner(""));
OrderController orderCtrl = new OrderController(
    sampleRepo, orderRepo, prodLineCtrl, new OrderView(), scanner);
```

| 케이스 | 검증 내용 |
|--------|---------|
| `placeOrder()` 정상 흐름 | RESERVED 주문 저장, `[성공]` 출력 |
| `placeOrder()` 존재하지 않는 sampleId | `[오류]` 출력, 저장 안 됨 |
| `placeOrder()` 빈 sampleId 입력 (`""`) | `[오류]` 출력 (findById empty → 오류 처리) |
| `placeOrder()` 빈 고객명 | `[오류]` 출력, 저장 안 됨 |
| `placeOrder()` 수량 파싱 실패 | `[오류]` 출력, 저장 안 됨 |
| `placeOrder()` 수량 0 이하 | `[오류]` 출력, 저장 안 됨 |
| `approveOrder()` RESERVED 없음 | "없습니다" 출력 후 반환 |
| `approveOrder()` 재고 충분 | 재고 차감, 주문 CONFIRMED, `CONFIRMED` 포함 출력 |
| `approveOrder()` 재고 부족 | 주문 PRODUCING, 큐에 항목 추가, `PRODUCING` 포함 출력 |
| `approveOrder()` 재고 == 수량 (경계값) | 재고 충분으로 처리 (CONFIRMED), 재고 0 |
| `approveOrder()` 잘못된 번호 | `[오류]` 출력, 상태 변경 없음 |
| `rejectOrder()` 정상 거절 | 주문 REJECTED, `[성공]` 출력 |
| `rejectOrder()` RESERVED 없음 | "없습니다" 출력 후 반환 |
| `rejectOrder()` 잘못된 번호 | `[오류]` 출력, 상태 변경 없음 |
| orderId 자동 생성 포맷 | `ORD-YYYYMMDD-NNNN` 형식 확인 |

---

## 8. 완료 기준

- [ ] `./gradlew test` 전체 통과 (Phase 1+2 회귀 없음)
- [ ] FR-ORD-01: 주문 접수 → RESERVED 저장, orderId 자동 생성
- [ ] FR-ORD-02: 재고 충분(CONFIRMED+차감) / 재고 부족(PRODUCING+큐 등록) 두 경로 모두 통과
- [ ] FR-ORD-02 경계값: 재고 == 주문 수량 → CONFIRMED
- [ ] FR-ORD-03: 주문 거절 → REJECTED, 재고 변동 없음
- [ ] FR-PROD-01: 생산 큐 목록 출력 (빈 큐 / 항목 있음)
- [ ] FR-PROD-02: 생산 완료 → 재고 증가 + CONFIRMED 전이 + 큐 항목 삭제
- [ ] 수율 계산 경계값 3케이스 통과
- [ ] 수율 0.0 시료 생산 큐 등록 시 `[오류]` 처리 확인
