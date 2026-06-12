# PRD — S-Semi 시료 생산주문관리 시스템

**문서 버전**: 1.1.0  
**최초 작성**: 2026-06-12  
**최종 수정**: 2026-06-12  
**프로젝트**: SampleOrderSystem (`SSemi` 본 프로젝트)

---

## 1. 제품 개요

### 1.1 목적

가상의 반도체 회사 **S-Semi**의 내부 운영 도구로,
연구소·팹리스·대학 연구실 등 외부 고객의 시료(Sample) 생산 주문을
**접수 → 승인 → 생산 → 출고**의 흐름으로 관리한다.

### 1.2 사용자

| 사용자 유형 | 설명 |
|------------|------|
| 운영 담당자 | 주문 접수, 승인/거절, 출고 처리를 수행하는 S-Semi 내부 직원 |

### 1.3 범위

- **포함**: 시료 관리, 주문 처리, 재고 모니터링, 생산 라인 관리, 출고 처리
- **제외**: 사용자 인증/권한, 네트워크 통신, GUI, 실시간 알림

---

## 2. 도메인 모델

### 2.1 시료 (Sample)

| 속성 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `sampleId` | `String` | 필수, 고유 | 고유 식별자 |
| `name` | `String` | 필수, 비공백 | 시료 이름 |
| `avgProductionTime` | `int` | > 0 | 평균 생산 시간(분) |
| `yield` | `double` | 0.0 < yield ≤ 1.0 | 수율 (정상 시료 / 총 생산 시료) |
| `stock` | `int` | ≥ 0 | 현재 재고 수량 |

### 2.2 주문 (Order)

| 속성 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `orderId` | `String` | 필수, 고유 | 고유 식별자 |
| `sampleId` | `String` | 존재하는 시료 ID | 주문 시료 |
| `customerName` | `String` | 필수, 비공백 | 고객명 |
| `quantity` | `int` | > 0 | 주문 수량 |
| `status` | `OrderStatus` | 필수 | 주문 상태 |

### 2.3 주문 상태 (OrderStatus)

```
RESERVED   → 주문 접수
PRODUCING  → 승인 완료, 재고 부족으로 생산 중
CONFIRMED  → 승인 완료, 출고 대기
RELEASE    → 출고 완료
REJECTED   → 주문 거절 (모니터링 제외)
```

#### 상태 전이 규칙

| 현재 상태 | 조건 | 전이 후 상태 |
|----------|------|-------------|
| `RESERVED` | 승인, 재고 ≥ 주문 수량 | `CONFIRMED` |
| `RESERVED` | 승인, 재고 < 주문 수량 | `PRODUCING` + 생산 라인 등록 |
| `RESERVED` | 거절 | `REJECTED` |
| `PRODUCING` | 생산 완료 | `CONFIRMED` |
| `CONFIRMED` | 출고 실행 | `RELEASE` |

### 2.4 생산 큐 항목 (ProductionQueueItem)

| 속성 | 타입 | 설명 |
|------|------|------|
| `queueId` | `String` | 고유 식별자 |
| `orderId` | `String` | 연결된 주문 ID |
| `sampleId` | `String` | 생산할 시료 ID |
| `requiredQuantity` | `int` | 부족 수량 = 주문 수량 - 재고 (생산 목표, 항상 양수) |
| `actualProductionQuantity` | `int` | 실 생산량 (수율 반영) |
| `totalProductionTime` | `int` | 총 생산 시간(분) |
| `enqueuedAt` | `String` | 큐 등록 시각 (ISO-8601) |

---

## 3. 기능 요구사항

### 3.1 메인 메뉴

메인 화면에는 시스템 현황(등록 시료 수, 총 재고, 전체 주문 수, 생산라인 대기 수)을 표시한다. 상세 레이아웃은 Section 8.2 참조.

```
[1] 시료 관리          [2] 시료 주문
[3] 주문 승인/거절     [4] 모니터링
[5] 생산라인 조회      [6] 출고 처리
[0] 종료
```

| 메뉴 | 기능 |
|------|------|
| [1] 시료 관리 | 시료 등록, 목록 조회, 이름 검색 |
| [2] 시료 주문 | 주문 접수 (RESERVED 생성) |
| [3] 주문 승인/거절 | 주문 승인(CONFIRMED/PRODUCING), 거절(REJECTED) |
| [4] 모니터링 | 상태별 주문 수, 시료별 재고 현황 |
| [5] 생산라인 조회 | 생산 큐 현황, 생산 완료 처리 |
| [6] 출고 처리 | CONFIRMED 주문 출고(RELEASE) |
| [0] 종료 | 애플리케이션 종료 |

---

### 3.2 시료 관리 (FR-SAM)

#### FR-SAM-01 시료 등록

- 입력: `sampleId`, `name`, `avgProductionTime`, `yield`, 초기 `stock`
- `sampleId` 중복 시 오류 메시지 출력 후 재입력
- 등록 완료 후 시료 정보 확인 출력

#### FR-SAM-02 시료 목록 조회

- 등록된 모든 시료를 테이블 형식으로 출력
- 컬럼: sampleId, 이름, 평균 생산시간, 수율, 재고

#### FR-SAM-03 시료 이름 검색

- 입력: 검색어 (부분 일치)
- 일치하는 시료 목록 출력
- 결과 없을 시 안내 메시지 출력

---

### 3.3 주문 처리 (FR-ORD)

#### FR-ORD-01 주문 접수

- 입력: `sampleId`, `customerName`, `quantity`
- 존재하지 않는 `sampleId` 입력 시 오류
- 생성된 주문의 초기 상태: `RESERVED`
- `orderId`는 시스템이 자동 생성

#### FR-ORD-02 주문 승인

- `RESERVED` 상태 주문 목록에서 선택
- **재고 충분** (재고 ≥ 주문 수량):
  - 재고에서 주문 수량 차감
  - 상태 → `CONFIRMED`
- **재고 부족** (재고 < 주문 수량):
  - 상태 → `PRODUCING`
  - 생산 큐에 항목 자동 등록 (`ProductionLineController`가 수행)
  - 실 생산량·총 생산 시간 계산은 `ProductionLineController`가 담당
    - 실 생산량 = `ceil(부족분 / (yield × 0.9))`
    - 총 생산 시간 = `avgProductionTime × 실 생산량`

#### FR-ORD-03 주문 거절

- `RESERVED` 상태 주문 목록에서 선택
- 상태 → `REJECTED`
- 재고 변동 없음

---

### 3.4 모니터링 (FR-MON)

#### FR-MON-01 상태별 주문 현황

- `REJECTED` 제외한 상태별(RESERVED / PRODUCING / CONFIRMED / RELEASE) 주문 수 표시

#### FR-MON-02 시료별 재고 현황

- 각 시료의 현재 재고와 재고 상태 표시
- 재고 상태 판정:
  - **여유**: 재고 > 0 이고 RESERVED/PRODUCING 주문 수량 합산 대비 충분
  - **부족**: 재고 > 0 이지만 주문 대비 부족
  - **고갈**: 재고 = 0

---

### 3.5 출고 처리 (FR-REL)

#### FR-REL-01 출고 실행

- `CONFIRMED` 상태 주문 목록 표시
- 선택한 주문의 상태 → `RELEASE`
- 출고 완료 메시지 출력

---

### 3.6 생산 라인 (FR-PROD)

#### FR-PROD-01 생산 현황 조회

- 현재 생산 큐 전체 목록 출력
- 컬럼: 순번(FIFO), sampleId, 시료명, 부족 수량, 실 생산량, 총 생산 시간, 연결 주문 ID

#### FR-PROD-02 생산 완료 처리

- 큐에서 선택한 항목을 완료 처리
- 시료 재고 += 실 생산량
- 해당 주문 상태 `PRODUCING` → `CONFIRMED`
- 완료된 큐 항목 제거

---

## 4. 비기능 요구사항

### 4.1 데이터 영속성

- 모든 데이터(시료, 주문, 생산 큐)는 JSON 파일로 저장한다
- 애플리케이션 재시작 후에도 데이터가 유지되어야 한다
- 저장 경로: `src/main/resources/data/`

### 4.2 성능

- 콘솔 반응 시간: 사용자 입력 후 1초 이내 화면 갱신

### 4.3 신뢰성

- 잘못된 입력(숫자 자리에 문자, 범위 초과 등)에 대해 오류 메시지를 출력하고 재입력 요청

### 4.4 유지보수성

- Model·Controller 핵심 로직 테스트 커버리지 80% 이상
- MVC 레이어 규칙 준수 (상위 CLAUDE.md 참조)

---

## 5. 기술 스택

| 항목 | 선택 |
|------|------|
| 언어 | Java 17+ |
| 빌드 | Gradle 8.x |
| JSON 직렬화 | Gson 2.11.0 |
| 테스트 | JUnit Jupiter 6.0.0 |
| 커버리지 | JaCoCo |
| 런타임 | 콘솔 (표준 입출력) |

---

## 6. PoC 통합 계획

### 6.1 핵심 설계 결정

| 결정 사항 | 선택 | 근거 |
|----------|------|------|
| Entity ID 타입 | `String` | JSON 영속성(DataPersistence) 요구사항 우선 |
| Entity 구조 | Getter/Setter (DataPersistence 방식) | Gson 역직렬화에 no-arg 생성자 + setter 필요 |
| Repository 기반 | DataPersistence 인터페이스 | CRUD 완전 지원 |
| 스레드 기반 모니터링 | 제거 | 단일 콘솔 앱, 메뉴 선택 시 단건 조회로 대체 |

### 6.2 ConsoleMVC 이식

**검증된 패턴**: MVC 계층 분리, Scanner 기반 입력, 생성자 주입, 테이블 포맷 출력

| 이식 대상 | 변경 사항 |
|---------|---------|
| `SampleController` | Long ID → String ID, `InMemorySampleRepository` → `JsonSampleRepository`, `update`/`delete` 기능 제거(PRD 범위 외) |
| `SampleView` | 메뉴 항목 조정 (update/delete 제거), 테이블 포맷은 그대로 재사용 |
| `Router` | 단일 Controller → 5개 Controller 주입, 6개 메뉴로 확장 |
| `InMemorySampleRepository` | **제거** — JSON 구현체로 대체 |
| `Sample` (엔티티) | **제거** — DataPersistence 버전(Getter/Setter)으로 대체 |

**재사용 테스트**: `SampleViewTest`의 출력 캡처 패턴(ByteArrayOutputStream), `SampleControllerTest`의 Spy View 패턴, `RouterTest`의 Mock 패턴

### 6.3 DataPersistence 이식

**검증된 패턴**: Gson 직렬화/역직렬화, Path 기반 파일 I/O, 중복 ID 검사, NoSuchElementException 예외

| 이식 대상 | 변경 사항 |
|---------|---------|
| `Sample`, `Order`, `OrderStatus` | 패키지 변경만 (`org.ssemi.persistence.model` → `org.ssemi.model.entity`) |
| `SampleRepository` (interface) | `findByNameContaining(String keyword): List<Sample>` 추가 |
| `OrderRepository` (interface) | `findBySampleId(String sampleId): List<Order>` 추가 |
| `JsonSampleRepository` | 패키지 변경, `findByNameContaining` 구현 추가 |
| `JsonOrderRepository` | 패키지 변경, `findBySampleId` 구현 추가 |
| `JsonFileUtil` | 패키지 변경만 (`org.ssemi.persistence.util` → `org.ssemi.model.repository`) |

**재사용 테스트**: `JsonSampleRepositoryTest`·`JsonOrderRepositoryTest`의 `@TempDir` 패턴, `JsonFileUtilTest` 전체

### 6.4 DataMonitor 이식

**검증된 패턴**: `calcStockLevel` 재고 상태 로직, ANSI 색상 출력, 상태별 카운팅(`Collectors.groupingBy`)

| 이식 대상 | 변경 사항 |
|---------|---------|
| `SampleStatus` (record) | `long sampleId` → `String sampleId` |
| `MonitoringController` | Long ID → String ID, `InMemoryRepository` → JSON Repository, `MonitoringLoop` 제거 |
| `MonitoringView` | 패키지 변경만, ANSI 색상 포함 그대로 재사용 |
| `MonitoringLoop` | **제거** — 스레드 불필요 |
| `InMemorySampleRepository`, `InMemoryOrderRepository` | **제거** — JSON 구현체로 대체 |
| `Sample`, `Order`, `OrderStatus` (DataMonitor 버전) | **제거** — DataPersistence 버전으로 통일 |

**재사용 테스트**: `MonitoringControllerTest`의 SpyMonitoringView + StubRepository 패턴, `calcStockLevel` 경계값 케이스

### 6.5 DummyDataGenerator 이식 (테스트 전용)

**검증된 패턴**: 시드 기반 재현 가능 데이터 생성, 상태 분포 배분 알고리즘

| 이식 대상 | 변경 사항 |
|---------|---------|
| `SampleGenerator` | `src/test/java/org/ssemi/fixture/SampleFixture.java`로 이식, `GeneratorConfig` 제거 후 간소화, ID 타입 Long→String (`"S-001"` 포맷) |
| `OrderGenerator` | `src/test/java/org/ssemi/fixture/OrderFixture.java`로 이식, `GeneratorConfig` 제거 후 간소화, ID 타입 Long→String (`"ORD-YYYYMMDD-NNNN"` 포맷) |
| `GsonFileWriter` | **제거** — `JsonFileUtil`로 통합 |
| `GeneratorConfig`, `DataGeneratorFacade` | **제거** — 테스트 픽스처에 불필요 |
| `GeneratorView` | **제거** — 테스트 코드에 뷰 불필요 |

### 6.6 신규 작성 목록

| 클래스 | 레이어 | 설명 |
|--------|--------|------|
| `ProductionQueueItem` | Entity | 생산 큐 항목 (queueId, orderId, sampleId, requiredQty, actualQty, totalTime, enqueuedAt) |
| `ProductionQueueRepository` | Repository interface | enqueue / findAll / findById / deleteById |
| `JsonProductionQueueRepository` | Repository impl | JSON 파일 기반 구현, `JsonFileUtil` 재사용 |
| `OrderController` | Controller | 주문 접수(RESERVED), 승인(CONFIRMED/PRODUCING), 거절(REJECTED) |
| `ReleaseController` | Controller | CONFIRMED → RELEASE 출고 처리 |
| `ProductionLineController` | Controller | 생산 큐 조회, 생산 완료 처리, `ceil(부족분 / (yield × 0.9))` 계산 |
| `MainView` | View | 메인 메뉴 출력, 시료 요약(총 수·부족·고갈 개수) |
| `OrderView` | View | 주문 목록 출력, 승인/거절 프롬프트 |
| `ReleaseView` | View | CONFIRMED 주문 목록, 출고 확인 |
| `ProductionLineView` | View | 생산 큐 테이블 출력 |
| `Main` | Entry point | 전체 DI 조립, `Router` 실행 |

---

## 7. 용어 정의

| 용어 | 정의 |
|------|------|
| 시료 (Sample) | S-Semi가 생산하여 고객에게 제공하는 반도체 시료 |
| 수율 (Yield) | 전체 생산량 대비 정상 시료 비율 (0.0 ~ 1.0) |
| 실 생산량 | 수율과 안전 마진(×0.9)을 반영한 실제 제조 목표량 |
| 생산 큐 | FIFO 방식으로 처리되는 생산 대기 목록 |
| 재고 고갈 | 시료 재고 수량이 0인 상태 |
| 재고 부족 | 재고가 0보다 크지만 대기 주문 수량 대비 부족한 상태 |

---

## 8. UI 화면 명세

> PDF 과제 명세서의 화면 목업을 기반으로 한다.

### 8.1 공통 규칙

| 항목 | 규격 |
|------|------|
| 구분선 | `=` 63개 (`===============================================================`) |
| 메뉴 형식 | `[N] 메뉴명` |
| 입력 프롬프트 | `선택 > ` |
| 수량 단위 | `ea` |
| 생산 시간 단위 | `min/ea` (시료 목록), `min` (총 시간) |
| 타임스탬프 형식 | `YYYY-MM-DD HH:MM:SS` |

#### ID 형식

| 항목 | 형식 | 예시 |
|------|------|------|
| 시료 ID | `S-NNN` | `S-001` |
| 주문 ID | `ORD-YYYYMMDD-NNNN` | `ORD-20250416-0043` |
| 큐 ID | `Q-NNN` | `Q-001` |

### 8.2 메인 화면

```
===============================================================
반도체 시료 생산주문관리 시스템
시스템 현황  2025-04-16 09:32:15

등록 시료 |12종    출 재고    2,540 ea
전체 주문  36건    생산라인   32건 대기

[1] 시료 관리          [2] 시료 주문
[3] 주문 승인/거절     [4] 모니터링
[5] 생산라인 조회      [6] 출고 처리
[0] 종료

선택 > _
```

- "출 재고": 전체 시료 재고 합산
- "생산라인 건 대기": 생산 큐 항목 수

### 8.3 시료 목록 화면

```
ID      시료명                평균 생산시간    수율    현재 재고
───────────────────────────────────────────────────────────
S-001   실리콘 웨이퍼-8인치   0.5 min/ea     0.92    480 ea
S-002   SiC 파워기판-6인치    1.2 min/ea     0.85    30 ea
```

### 8.4 주문 승인 상세 화면

```
시료  SiC 파워기판-6인치  현재 재고 30 ea  주문 수량 200 ea  부족분 170 ea
실생산량 205 ea / 165 min
상태 변경  RESERVED → PRODUCING
```

- 재고 충분일 때: `상태 변경  RESERVED → CONFIRMED`

### 8.5 생산 큐 화면

```
순서  주문번호           시료                  주문량    부족분    실생산량   예상 완료
────────────────────────────────────────────────────────────────────────────────
1     ORD-20250416-0040  산화막 웨이퍼-SiO2    150 ea    150 ea    190 ea    11:43
2     ORD-20250416-0041  실리콘 웨이퍼-8인치   200 ea    170 ea    205 ea    14:21

* 부족분 = 주문량 - 재고, 실생산량 = ceil(부족분 / (수율 * 0.9))
```

- 예상 완료 시각: `HH:mm` 형식, `enqueuedAt + totalProductionTime(분)` 계산

### 8.6 ANSI 색상 규칙

| 상태 / 수준 | 색상 |
|------------|------|
| `RESERVED` | 파란 (Blue) |
| `CONFIRMED` | 초록 (Green) |
| `PRODUCING` | 주황 (Yellow) |
| `REJECTED` | 빨간 (Red) |
| `RELEASE` | 보라 (Purple/Magenta) |
| 재고 여유 | 초록 (Green) |
| 재고 부족 | 주황 (Yellow) |
| 재고 고갈 | 빨간 (Red) |

### 8.7 주문 접수 확인 단계

주문 접수 전 입력 정보를 요약 출력하고 Y/N 확인을 받는다.

```
[주문 확인]
시료: 실리콘 웨이퍼-8인치 (S-001)
고객: 서울대학교 연구실
수량: 100 ea

진행하시겠습니까? (Y/N) > _
```
