# SampleOrderSystem — S-Semi 시료 생산주문관리 시스템

> 상위 규칙: `과제/CLAUDE.md` 상속. 이 파일은 추가·재정의 규칙만 기술한다.

---

## 워크플로우 규칙

> 전역 규칙(`C:/Users/User/.claude/CLAUDE.md`)을 먼저 읽고 아래 규칙과 함께 적용한다.

### Plan · Phase 설계 완료 후 자동 실행

plan 또는 phase 설계 문서를 생성·완료한 직후 반드시 아래 순서로 실행한다.

1. **`.claude/agents/doc-consistency`** — 문서 교차 참조·정합성 검증 (단독)
2. **`.claude/agents/test-verify`** + **`.claude/agents/compliance-verify`** — doc-consistency 완료 후 병렬 실행

각 agent 완료 후 결과를 한 줄 요약으로 사용자에게 보고한다.
문제가 발견된 경우 `docs/reports/` 경로의 보고서 파일명을 명시한다.

### 구현 요청 시

사용자가 "구현해", "implement", 또는 이에 준하는 요청을 하면
**`.claude/agents/ai-code-gen`** agent를 실행해 phase 설계 문서 기반으로 코드를 직접 생성한다.

### 명시적 호출

사용자가 agent 이름을 직접 언급하면 즉시 해당 agent를 실행한다.
예) "doc-consistency 돌려줘", "test-verify 해줘", "compliance 체크해", "ai-code-gen 실행해"

---

## 프로젝트 정체

`SSemi` 본 프로젝트. 4개 PoC의 검증 결과를 통합한 **Java 콘솔 애플리케이션**이다.

- **루트 패키지**: `org.ssemi`
- **Gradle 프로젝트명**: `SampleOrderSystem`

---

## 현재 상태 (2026-06-12)

| 항목 | 상태 |
|------|------|
| Gradle 기본 설정 | 완료 |
| 소스 코드 | 미작성 |
| JSON 라이브러리 의존성 | 미추가 |
| Jacoco 플러그인 | 미추가 |
| docs/PRD.md | 작성 완료 (v1.1.0) |
| docs/PLAN.md | 작성 완료 (v1.1.0) |

---

## build.gradle 필수 추가 항목

구현 시작 전 아래 항목을 `build.gradle`에 반드시 추가한다.

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
    reports {
        xml.required = true
        html.required = true
    }
}
```

---

## 핵심 설계 결정 사항

### ID 타입: `String` 통일

DataPersistence는 `String`, DataMonitor·DummyDataGenerator는 `Long`을 사용한다.
본 프로젝트는 **JSON 파일 영속성** 요구사항에 맞춰 **`String`으로 통일**한다.

- `"S-001"`, `"ORD-YYYYMMDD-NNNN"`, `"Q-001"` 형태의 접두사+순번 포맷 사용 (PRD Section 8.1 기준)
- 이식 시 `Long` → `String` 타입 교체 필수

### Entity 설계: DataPersistence 기준 (Getter/Setter)

Gson 역직렬화를 위해 no-arg 생성자 + setter 구조를 유지한다.
DataMonitor의 불변(Immutable) 엔티티 방식은 **사용하지 않는다**.

### SampleRepository 인터페이스: DataPersistence 기반 + ConsoleMVC 확장

DataPersistence의 인터페이스를 베이스로,
ConsoleMVC의 `findByNameContaining(String)` 메서드를 추가한다.

### OrderRepository 인터페이스: DataPersistence 기반 + DataMonitor 확장

DataPersistence의 인터페이스를 베이스로,
DataMonitor의 `findBySampleId(String)` 메서드를 추가한다 (Long → String 변환).

---

## PoC 이식 상세 계획

### ConsoleMVC → 이식 대상

| 원본 (org.example) | 이식 (org.ssemi) | 변경 사항 |
|-------------------|-----------------|---------|
| `model/entity/Sample.java` | 사용 안 함 | DataPersistence 버전으로 대체 |
| `model/repository/SampleRepository.java` | `model/repository/SampleRepository.java` | findByNameContaining 유지, ID 타입 String으로 변경 |
| `model/repository/InMemorySampleRepository.java` | 사용 안 함 | JSON 구현체로 대체 |
| `controller/SampleController.java` | `controller/SampleController.java` | ID 타입 Long→String, JSON repo 사용, update/delete 제거 (PRD 범위 외) |
| `view/SampleView.java` | `view/SampleView.java` | 테이블 포맷 재사용, 메뉴 항목 조정 |
| `app/Router.java` | `app/Router.java` | 6개 메뉴로 확장, 복수 Controller 주입 |

### DataPersistence → 이식 대상

| 원본 (org.ssemi.persistence) | 이식 (org.ssemi) | 변경 사항 |
|-----------------------------|-----------------|---------|
| `model/Sample.java` | `model/entity/Sample.java` | 패키지 변경만 |
| `model/Order.java` | `model/entity/Order.java` | 패키지 변경만 |
| `model/OrderStatus.java` | `model/entity/OrderStatus.java` | 패키지 변경만 |
| `repository/SampleRepository.java` | `model/repository/SampleRepository.java` | findByNameContaining(String) 추가 |
| `repository/OrderRepository.java` | `model/repository/OrderRepository.java` | findBySampleId(String) 추가 |
| `repository/JsonSampleRepository.java` | `model/repository/JsonSampleRepository.java` | 패키지 변경만 |
| `repository/JsonOrderRepository.java` | `model/repository/JsonOrderRepository.java` | findBySampleId 구현 추가 |
| `util/JsonFileUtil.java` | `model/repository/JsonFileUtil.java` | 패키지 변경만 |

### DataMonitor → 이식 대상

| 원본 (org.example) | 이식 (org.ssemi) | 변경 사항 |
|-------------------|-----------------|---------|
| `model/entity/SampleStatus.java` | `model/entity/SampleStatus.java` | record의 `long sampleId` → `String sampleId` |
| `model/entity/OrderStatus.java` | 사용 안 함 | DataPersistence 버전으로 통일 |
| `model/entity/Sample.java` | 사용 안 함 | DataPersistence 버전으로 통일 |
| `model/entity/Order.java` | 사용 안 함 | DataPersistence 버전으로 통일 |
| `controller/MonitoringController.java` | `controller/MonitoringController.java` | ID 타입 Long→String, 스레드 루프 제거, JSON repo 사용 |
| `view/MonitoringView.java` | `view/MonitoringView.java` | 패키지 변경, 그대로 재사용 |
| `app/MonitoringLoop.java` | 사용 안 함 | 단일 콘솔 앱이므로 스레드 불필요 |
| `model/repository/InMemory*.java` | 사용 안 함 | JSON 구현체로 대체 |

### DummyDataGenerator → 이식 대상 (테스트 전용)

| 원본 (org.example) | 이식 (org.ssemi) | 변경 사항 |
|-------------------|-----------------|---------|
| `generator/SampleGenerator.java` | `src/test/java/org/ssemi/fixture/SampleFixture.java` | ID 타입 Long→String ("S001"...), GeneratorConfig 제거 후 간소화 |
| `generator/OrderGenerator.java` | `src/test/java/org/ssemi/fixture/OrderFixture.java` | ID 타입 Long→String ("O001"...), GeneratorConfig 제거 후 간소화 |
| `writer/GsonFileWriter.java` | 사용 안 함 | JsonFileUtil로 통합 |
| `config/GeneratorConfig.java` | 사용 안 함 | 테스트 픽스처에 필요 없음 |

### 신규 작성 (어떤 PoC에도 없는 클래스)

| 파일 | 설명 |
|------|------|
| `model/entity/ProductionQueueItem.java` | 생산 큐 항목 엔티티 |
| `model/repository/ProductionQueueRepository.java` | 생산 큐 저장소 인터페이스 |
| `model/repository/JsonProductionQueueRepository.java` | 생산 큐 JSON 구현체 |
| `controller/OrderController.java` | 주문 접수·승인·거절 처리 |
| `controller/ReleaseController.java` | 출고 처리 |
| `controller/ProductionLineController.java` | 생산 라인 관리 + 수율 계산 |
| `view/MainView.java` | 메인 메뉴 + 시료 요약 |
| `view/OrderView.java` | 주문 관련 화면 |
| `view/ReleaseView.java` | 출고 화면 |
| `view/ProductionLineView.java` | 생산 라인 화면 |
| `Main.java` | DI 조립 + Router 실행 |

---

## 패키지 구조

```
src/main/java/org/ssemi/
├── Main.java                                      (신규)
├── model/
│   ├── entity/
│   │   ├── Sample.java                            (DataPersistence 이식)
│   │   ├── Order.java                             (DataPersistence 이식)
│   │   ├── OrderStatus.java                       (DataPersistence 이식)
│   │   ├── SampleStatus.java                      (DataMonitor 이식, String ID로 변경)
│   │   └── ProductionQueueItem.java               (신규)
│   └── repository/
│       ├── SampleRepository.java                  (DataPersistence + findByNameContaining 추가)
│       ├── OrderRepository.java                   (DataPersistence + findBySampleId 추가)
│       ├── ProductionQueueRepository.java         (신규)
│       ├── JsonSampleRepository.java              (DataPersistence 이식)
│       ├── JsonOrderRepository.java               (DataPersistence 이식 + findBySampleId 구현)
│       ├── JsonProductionQueueRepository.java     (신규)
│       └── JsonFileUtil.java                      (DataPersistence 이식)
├── controller/
│   ├── SampleController.java                      (ConsoleMVC 이식, String ID·JSON repo 적용)
│   ├── OrderController.java                       (신규)
│   ├── MonitoringController.java                  (DataMonitor 이식, String ID·JSON repo 적용)
│   ├── ReleaseController.java                     (신규)
│   └── ProductionLineController.java              (신규, 수율 계산 포함)
├── view/
│   ├── MainView.java                              (신규)
│   ├── SampleView.java                            (ConsoleMVC 이식, 메뉴 항목 조정)
│   ├── OrderView.java                             (신규)
│   ├── MonitoringView.java                        (DataMonitor 이식, 그대로 재사용)
│   ├── ReleaseView.java                           (신규)
│   └── ProductionLineView.java                    (신규)
└── app/
    └── Router.java                                (ConsoleMVC 이식, 5메뉴로 확장)

src/main/resources/data/
├── samples.json
├── orders.json
└── production_queue.json

src/test/java/org/ssemi/
├── fixture/
│   ├── SampleFixture.java    (DummyDataGenerator SampleGenerator 간소화 이식)
│   └── OrderFixture.java     (DummyDataGenerator OrderGenerator 간소화 이식)
├── model/entity/
├── model/repository/
├── controller/
├── view/
└── integration/
```

---

## 인터페이스 최종 명세

### SampleRepository

```java
void save(Sample sample);
Optional<Sample> findById(String sampleId);
List<Sample> findAll();
List<Sample> findByNameContaining(String keyword);   // ConsoleMVC 추가
void update(Sample sample);                          // DataPersistence 이식 (현재 PRD 기능에서 미사용, 데이터 무결성 유지용)
void deleteById(String sampleId);                    // DataPersistence 이식 (현재 PRD 기능에서 미사용, 데이터 무결성 유지용)
```

### OrderRepository

```java
void save(Order order);
Optional<Order> findById(String orderId);
List<Order> findAll();
List<Order> findByStatus(OrderStatus status);
List<Order> findBySampleId(String sampleId);         // DataMonitor 추가 (Long→String 변환)
void update(Order order);
// deleteById 미포함 — PRD에 주문 삭제 기능 없음, REJECTED는 상태 전이로 처리
```

### ProductionQueueRepository

```java
void enqueue(ProductionQueueItem item);
List<ProductionQueueItem> findAll();
Optional<ProductionQueueItem> findById(String queueId);
void deleteById(String queueId);
```

---

## 코딩 컨벤션 (상위 CLAUDE.md 보완)

- ID 자동 생성 규칙: `System.currentTimeMillis()` 또는 순번 기반 (`"S" + String.format("%03d", n)`)
- `ProductionQueueItem`의 `enqueuedAt`은 `LocalDateTime.now().toString()` 사용
- `Main.java`는 의존성 조립(DI)과 `Router` 실행만 담당 — 비즈니스 로직 금지
- `Router.java`는 메뉴 루프와 Controller 위임만 담당
- 수율 계산식 `ceil(부족분 / (yield × 0.9))`는 `ProductionLineController`에만 위치
- Entity ID 타입은 `String`으로 통일

---

## JSON 데이터 파일 경로 규칙

- 런타임 데이터: `src/main/resources/data/*.json`
- 테스트 픽스처: `src/test/resources/data/*.json`
- Repository 구현체는 파일 경로를 생성자 주입으로 받아 테스트 시 교체 가능하게 한다
