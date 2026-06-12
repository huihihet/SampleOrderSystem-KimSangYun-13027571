# Phase 1 설계 문서 — 도메인 모델 레이어

**문서 버전**: 1.0.0  
**작성일**: 2026-06-12  
**참조**: [PLAN.md](../PLAN.md) Phase 1, [PRD.md](../PRD.md)  
**선행 Phase**: Phase 0 (build.gradle + 빈 JSON 파일)

---

## 1. 목표

Entity 5종, Repository 인터페이스 3종, JSON 구현체 3종, JsonFileUtil을 구현한다.  
이후 Phase 2~5의 기반 레이어이므로 **이 Phase 완료 전 다른 Phase는 시작하지 않는다**.

---

## 2. 산출물 목록

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

src/test/java/org/ssemi/
├── model/entity/
│   ├── SampleTest.java
│   ├── OrderTest.java
│   ├── ProductionQueueItemTest.java
│   └── SampleStatusTest.java
└── model/repository/
    ├── JsonFileUtilTest.java
    ├── JsonSampleRepositoryTest.java
    ├── JsonOrderRepositoryTest.java
    └── JsonProductionQueueRepositoryTest.java
```

---

## 3. Entity 설계

### 3-1. `OrderStatus` (enum)

패키지: `org.ssemi.model.entity`  
출처: DataPersistence 이식 (패키지 변경만)

```java
package org.ssemi.model.entity;

public enum OrderStatus {
    RESERVED,
    PRODUCING,
    CONFIRMED,
    RELEASE,
    REJECTED
}
```

---

### 3-2. `Sample`

패키지: `org.ssemi.model.entity`  
출처: DataPersistence 이식 + 도메인 메서드 추가

**필드**:

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `sampleId` | `String` | 고유 식별자 (예: `"S-001"`) |
| `name` | `String` | 시료 이름 |
| `avgProductionTime` | `int` | 평균 생산 시간(분/개) |
| `yield` | `double` | 수율 (0.0 ~ 1.0) |
| `stock` | `int` | 현재 재고 수량 |

**생성자**:
- no-arg 생성자 (Gson 역직렬화용)
- 전체 인자 생성자: `Sample(String sampleId, String name, int avgProductionTime, double yield, int stock)`

**Getter/Setter**: 전 필드

**equals/hashCode**: `sampleId` 기반

**도메인 메서드**:

```java
// Controller에서 stock 직접 조작 금지 — 불변식(stock >= 0) 보장
public void deductStock(int quantity) {
    if (this.stock - quantity < 0) {
        throw new IllegalStateException(
            "재고 부족: 현재 재고 " + this.stock + " ea, 차감 요청 " + quantity + " ea"
        );
    }
    this.stock -= quantity;
}

// 생산 완료 시 호출
public void addStock(int quantity) {
    this.stock += quantity;
}
```

---

### 3-3. `Order`

패키지: `org.ssemi.model.entity`  
출처: DataPersistence 이식 (패키지 변경만)

**필드**:

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `orderId` | `String` | 고유 식별자 (예: `"ORD-20260612-0001"`) |
| `sampleId` | `String` | 주문 시료 ID |
| `customerName` | `String` | 고객명 |
| `quantity` | `int` | 주문 수량 |
| `status` | `OrderStatus` | 주문 상태 |

**생성자**:
- no-arg 생성자 (Gson 역직렬화용)
- 전체 인자 생성자: `Order(String orderId, String sampleId, String customerName, int quantity, OrderStatus status)`

**Getter/Setter**: 전 필드

**equals/hashCode**: `orderId` 기반

---

### 3-4. `SampleStatus` (record)

패키지: `org.ssemi.model.entity`  
출처: DataMonitor 이식 — `long sampleId` → `String sampleId` 변경

```java
package org.ssemi.model.entity;

public record SampleStatus(String sampleId, String name, int stock, String stockLevel) {}
```

---

### 3-5. `ProductionQueueItem`

패키지: `org.ssemi.model.entity`  
출처: 신규

**필드**:

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `queueId` | `String` | 고유 식별자 (예: `"Q-001"`) |
| `orderId` | `String` | 연결된 주문 ID |
| `sampleId` | `String` | 연결된 시료 ID |
| `requiredQuantity` | `int` | 부족 수량 = 주문 수량 - 재고 (항상 양수) |
| `actualProductionQuantity` | `int` | 생산 투입량 (ProductionLineController에서 계산 후 주입, 계산식: `ceil(requiredQuantity / (yield × 0.9))`) |
| `totalProductionTime` | `int` | 총 생산 시간 (ProductionLineController에서 계산 후 주입, 계산식: `avgProductionTime × actualProductionQuantity`) |
| `enqueuedAt` | `String` | 큐 등록 시각 (예: `LocalDateTime.now().toString()`) |

**생성자**:
- no-arg 생성자 (Gson 역직렬화용)
- 전체 인자 생성자

**Getter/Setter**: 전 필드

---

## 4. Repository 인터페이스 설계

### 4-1. `SampleRepository`

패키지: `org.ssemi.model.repository`  
출처: DataPersistence 이식 + `findByNameContaining` 추가

```java
package org.ssemi.model.repository;

import org.ssemi.model.entity.Sample;
import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String sampleId);
    List<Sample> findAll();
    List<Sample> findByNameContaining(String keyword);
    void update(Sample sample);
    void deleteById(String sampleId);
}
```

> `update`/`deleteById`: PRD 범위 외 기능이지만 데이터 무결성 유지용으로 인터페이스에 포함

---

### 4-2. `OrderRepository`

패키지: `org.ssemi.model.repository`  
출처: DataPersistence 이식 + `findBySampleId` 추가, `deleteById` 제거

```java
package org.ssemi.model.repository;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    List<Order> findBySampleId(String sampleId);
    void update(Order order);
}
```

> 주문 삭제 없음 — REJECTED는 상태 전이로 처리

---

### 4-3. `ProductionQueueRepository`

패키지: `org.ssemi.model.repository`  
출처: 신규

```java
package org.ssemi.model.repository;

import org.ssemi.model.entity.ProductionQueueItem;
import java.util.List;
import java.util.Optional;

public interface ProductionQueueRepository {
    void enqueue(ProductionQueueItem item);
    List<ProductionQueueItem> findAll();
    Optional<ProductionQueueItem> findById(String queueId);
    void deleteById(String queueId);
}
```

---

## 5. Repository 구현체 설계

### 5-1. `JsonFileUtil`

패키지: `org.ssemi.model.repository`  
출처: DataPersistence 이식 (패키지 변경만)

**역할**: JSON 파일 읽기/쓰기 유틸리티. Gson 사용.

```java
package org.ssemi.model.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonFileUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 파일 미존재·빈 파일도 빈 리스트로 정상 처리 (예외 미발생 계약)
    public static <T> List<T> readList(Path filePath, Type type) { ... }

    public static <T> void writeList(Path filePath, List<T> list) { ... }
}
```

**구현 주의사항**:
- 파일이 존재하지 않거나 내용이 `[]` 이면 빈 `ArrayList` 반환
- `IOException` 발생 시 `RuntimeException`으로 감싸 전파
- 파일 경로의 부모 디렉토리가 없으면 `Files.createDirectories()` 호출

---

### 5-2. `JsonSampleRepository`

패키지: `org.ssemi.model.repository`  
출처: DataPersistence 이식 + `findByNameContaining` 추가

**생성자**: `JsonSampleRepository(Path filePath)` — 테스트 시 `@TempDir` 경로 주입 가능

```java
// TypeToken: new TypeToken<List<Sample>>(){}.getType()

void save(Sample sample)
    // 중복 sampleId 있으면 IllegalArgumentException("이미 존재하는 시료 ID: " + sampleId)

Optional<Sample> findById(String sampleId)
    // findAll() 스트림에서 sampleId 일치하는 첫 번째 반환

List<Sample> findAll()
    // JsonFileUtil.readList() 호출

List<Sample> findByNameContaining(String keyword)
    // keyword가 null 또는 blank → findAll() 반환
    // 아니면 name.contains(keyword) 필터링

void update(Sample sample)
    // findAll()에서 sampleId 일치 항목을 교체 후 writeList()

void deleteById(String sampleId)
    // findAll()에서 sampleId 불일치 항목만 남겨 writeList()
```

---

### 5-3. `JsonOrderRepository`

패키지: `org.ssemi.model.repository`  
출처: DataPersistence 이식 + `findBySampleId` 추가

**생성자**: `JsonOrderRepository(Path filePath)`

```java
// TypeToken: new TypeToken<List<Order>>(){}.getType()

void save(Order order)
    // 중복 orderId 있으면 IllegalArgumentException

Optional<Order> findById(String orderId)

List<Order> findAll()

List<Order> findByStatus(OrderStatus status)
    // findAll() 스트림에서 status 일치 필터링

List<Order> findBySampleId(String sampleId)
    // findAll() 스트림에서 sampleId.equals(o.getSampleId()) 필터링
    // null 검증 없음 — 호출자 책임 (내부 API)

void update(Order order)
    // findAll()에서 orderId 일치 항목 교체 후 writeList()
```

---

### 5-4. `JsonProductionQueueRepository`

패키지: `org.ssemi.model.repository`  
출처: 신규. `JsonOrderRepository`와 동일한 구조.

**생성자**: `JsonProductionQueueRepository(Path filePath)`

```java
// TypeToken: new TypeToken<List<ProductionQueueItem>>(){}.getType()

void enqueue(ProductionQueueItem item)
    // 중복 queueId 있으면 IllegalArgumentException

List<ProductionQueueItem> findAll()

Optional<ProductionQueueItem> findById(String queueId)

void deleteById(String queueId)
    // queueId 불일치 항목만 남겨 writeList()
    // 존재하지 않는 queueId면 NoSuchElementException
```

---

## 6. 테스트 계획

### 6-1. Entity 테스트

#### `SampleTest`

| 케이스 | 설명 |
|--------|------|
| 전체 인자 생성자로 생성 후 getter 검증 | 정상 생성 확인 |
| no-arg 생성자 + setter 후 getter 검증 | Gson 역직렬화 경로 확인 |
| equals: 동일 sampleId → true | hashCode 포함 |
| equals: 다른 sampleId → false | |
| `deductStock(100)`: stock 300 → stock 200 | 정상 차감 |
| `deductStock(quantity)`: stock == quantity → stock 0 | 경계값: 정확히 소진 |
| `deductStock(quantity)`: stock < quantity → `IllegalStateException` | 재고 부족 예외 |
| `deductStock(0)`: stock 변화 없음 | 0 차감 허용 |
| `addStock(50)`: stock 200 → stock 250 | 정상 증가 |
| `addStock(0)`: stock 변화 없음 | 0 증가 허용 |

#### `OrderTest`

| 케이스 | 설명 |
|--------|------|
| 전체 인자 생성자 후 getter 검증 | `OrderStatus.RESERVED` 포함 |
| no-arg 생성자 + setter 검증 | |
| equals: 동일 orderId → true | |
| setStatus: `RESERVED` → `CONFIRMED` | 상태 변경 확인 |
| setStatus: 5개 상태 전체 각각 설정 후 getter 반환 | `PRODUCING`, `CONFIRMED`, `RELEASE`, `REJECTED` 포함 |
| `OrderStatus.values()` 길이 == 5 | enum 값 전체 정의 확인 |
| `OrderStatus.valueOf("PRODUCING")` 성공 | Gson 역직렬화 경로 (문자열 → enum) |

#### `ProductionQueueItemTest`

| 케이스 | 설명 |
|--------|------|
| 전체 인자 생성자 후 getter 검증 | 수치 필드(requiredQuantity, actualProductionQuantity, totalProductionTime) 포함 |
| no-arg 생성자 + setter 검증 | |

#### `SampleStatusTest`

| 케이스 | 설명 |
|--------|------|
| record 생성 및 필드 접근 | `sampleId`가 `String` 타입인지 확인 |
| equals: 동일 값 → true | record 기본 동등성 |

---

### 6-2. Repository 테스트

모든 Repository 테스트는 `@TempDir` 기반 독립 JSON 파일 사용.

#### `JsonFileUtilTest`

| 케이스 | 설명 |
|--------|------|
| 빈 리스트 writeList → readList → 빈 리스트 반환 | 왕복 직렬화 |
| 항목 있는 리스트 writeList → readList → 동일 내용 | |
| 존재하지 않는 파일 readList → 빈 리스트 반환 | 파일 없음 처리 |
| 부모 디렉토리 없는 경로에 writeList → 자동 생성 후 저장 성공 | |
| 손상된 JSON 파일(`"[{broken"`) readList → `RuntimeException` 계열 예외 | 파싱 오류 처리 |

#### `JsonSampleRepositoryTest`

| 케이스 | 설명 |
|--------|------|
| `save` 후 `findById` 조회 성공 | |
| `save` 후 `findAll` 목록에 포함 | |
| **영속성**: `save` 후 새 인스턴스 생성 → `findById` 동일 데이터 반환 | 재시작 후 데이터 유지 확인 |
| 중복 sampleId `save` → `IllegalArgumentException` | |
| `update`: 기존 항목 수정 후 재조회 | stock 변경 확인 |
| `update`: 존재하지 않는 ID → 예외 또는 데이터 손실 없음 확인 | 비존재 ID 안전 처리 |
| `deleteById`: 삭제 후 `findById` empty | |
| `findByNameContaining("웨이퍼")`: 이름 포함 항목만 반환 | |
| `findByNameContaining("")`: 전체 반환 | blank 처리 |
| `findByNameContaining(null)`: 전체 반환 | null 처리 |
| `findById` 존재하지 않는 ID → `Optional.empty()` | |

#### `JsonOrderRepositoryTest`

| 케이스 | 설명 |
|--------|------|
| `save` 후 `findById` 조회 성공 | |
| **영속성**: `save` 후 새 인스턴스 생성 → `findById` 동일 데이터 반환 | 재시작 후 데이터 유지 확인 |
| 중복 orderId `save` → `IllegalArgumentException` | |
| `findByStatus(RESERVED)`: 해당 상태 주문만 반환 | |
| `findByStatus(CONFIRMED)`: 다른 상태 제외 확인 | |
| `findBySampleId("S-001")`: 해당 시료 주문만 반환 | |
| `findBySampleId("S-999")`: 빈 리스트 반환 | 존재하지 않는 시료 |
| `update`: 상태 변경 후 재조회 | `RESERVED` → `CONFIRMED` |
| `update`: 존재하지 않는 orderId → 예외 또는 데이터 손실 없음 확인 | 비존재 ID 안전 처리 |

#### `JsonProductionQueueRepositoryTest`

| 케이스 | 설명 |
|--------|------|
| `enqueue` 후 `findAll` 목록에 포함 | |
| **영속성**: `enqueue` 후 새 인스턴스 생성 → `findAll` 동일 데이터 반환 | 재시작 후 데이터 유지 확인 |
| 중복 queueId `enqueue` → `IllegalArgumentException` | |
| `findById` 조회 성공 | |
| `findById` 존재하지 않는 ID → `Optional.empty()` | |
| `deleteById`: 삭제 후 `findAll`에 미포함 | |
| `deleteById` 존재하지 않는 ID → `NoSuchElementException` | |
| 순서 보장: enqueue 순서대로 `findAll` 반환 (FIFO) | |
| 빈 큐 `findAll` → 빈 리스트 | |

---

## 7. 완료 기준

- [ ] `./gradlew test` 전체 통과 (빨간 테스트 없음)
- [ ] 모든 Repository 테스트가 `@TempDir` 기반 독립 파일로 실행됨
- [ ] `Sample.deductStock()`: 정상 / 경계값(stock==quantity) / 예외(stock<quantity) 세 케이스 통과
- [ ] JSON Repository 3종 모두 영속성 보장 케이스 통과 (새 인스턴스 재조회)
- [ ] `JsonFileUtil`: 손상된 JSON 파일 처리 케이스 통과
- [ ] `update()`: 비존재 ID 처리 동작이 명세에 따라 검증됨
- [ ] `JsonProductionQueueRepository`: FIFO 순서 유지 확인
- [ ] Entity 클래스 5종 모두 no-arg 생성자 보유 (Gson 역직렬화 가능)
- [ ] `OrderStatus` 5개 값 전체 및 `valueOf()` 역직렬화 경로 검증
