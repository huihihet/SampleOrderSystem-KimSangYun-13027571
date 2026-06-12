# Phase 2 설계 문서 — 시료 관리 기능

**문서 버전**: 1.0.0  
**작성일**: 2026-06-12  
**참조**: [PLAN.md](../PLAN.md) Phase 2, [PRD.md](../PRD.md) FR-SAM-01~03  
**선행 Phase**: Phase 1 완료 필수

---

## 1. 목표

FR-SAM-01(시료 등록), FR-SAM-02(목록 조회), FR-SAM-03(이름 검색)을 구현한다.

---

## 2. 산출물 목록

```
src/main/java/org/ssemi/
├── controller/SampleController.java
└── view/SampleView.java

src/test/java/org/ssemi/
├── controller/SampleControllerTest.java
└── view/SampleViewTest.java
```

---

## 3. View 설계

### `SampleView`

패키지: `org.ssemi.view`  
출처: ConsoleMVC 이식 — update/delete 관련 메뉴·메서드 제거, 출력 포맷 조정

**메서드 목록**:

```java
void printMenu()
void printPrompt(String prompt)
void printSampleList(List<Sample> samples)
void printSuccess(String message)
void printError(String message)
void printEmpty()
```

#### `printMenu()`

```
[1] 시료 등록
[2] 목록 조회
[3] 이름 검색
[0] 뒤로
선택 > 
```

#### `printSampleList(List<Sample> samples)`

헤더 + 구분선 + 행 반복 형식:

```
ID       시료명                    평균생산시간   수율    현재재고
----------------------------------------------------------------
S-001    실리콘 웨이퍼-8인치       30 min/ea     0.92    480 ea
S-002    GaAs 기판                 72 min/ea     0.85    120 ea
```

- 구분선: 64개 `-` 문자 (테이블 내부 행 구분선. PRD 8.1의 `=` 63개는 메인 메뉴 구분선)
- 평균생산시간: `int` 타입 정수 + `min/ea` 단위 (예: `30 min/ea`)
- 수율: 소수점 2자리 (예: `0.92`)
- 현재재고 단위: `ea`

#### `printEmpty()`

```
등록된 시료가 없습니다.
```

#### `printSuccess(String message)` / `printError(String message)`

```
[성공] {message}
[오류] {message}
```

---

## 4. Controller 설계

### `SampleController`

패키지: `org.ssemi.controller`  
출처: ConsoleMVC 이식 — String ID, JsonSampleRepository, update/delete 제거

**생성자 (Constructor Injection)**:

```java
SampleController(SampleRepository repository, SampleView view, Scanner scanner)
```

**메서드**:

#### `void register()`

1. `view.printPrompt("시료 ID (예: S-001) > ")` 후 sampleId 입력
2. `view.printPrompt("시료 이름 > ")` 후 name 입력  
   → `name.isBlank()` 이면 `view.printError("시료 이름을 입력해 주세요.")` 후 반환
3. `view.printPrompt("평균 생산시간 (분, 정수) > ")` 후 avgProductionTime 입력
4. `view.printPrompt("수율 (0.0~1.0) > ")` 후 yield 입력
5. `view.printPrompt("초기 재고 수량 > ")` 후 stock 입력
6. `repository.findById(sampleId)`로 중복 확인 → 있으면 `view.printError("이미 존재하는 시료 ID입니다: " + sampleId)` 후 반환
7. `new Sample(sampleId, name, avgProductionTime, yield, stock)` 생성 후 `repository.save()`
8. `view.printSuccess("시료가 등록되었습니다: " + name)`

**입력 파싱 실패 처리**: `avgProductionTime` 또는 `stock`의 `NumberFormatException` 발생 시 `view.printError("올바른 숫자를 입력해 주세요.")` 후 반환  
**수율 범위 검증**: yield < 0.0 또는 yield > 1.0이면 `view.printError("수율은 0.0 ~ 1.0 사이여야 합니다.")` 후 반환  
**음수 재고 검증**: stock < 0이면 `view.printError("재고는 0 이상이어야 합니다.")` 후 반환  
**avgProductionTime 검증**: avgProductionTime <= 0이면 `view.printError("생산시간은 1 이상이어야 합니다.")` 후 반환

#### `void listAll()`

1. `repository.findAll()` 호출
2. 결과가 비어있으면 `view.printEmpty()`
3. 아니면 `view.printSampleList(samples)`

#### `void searchByName()`

1. `view.printPrompt("검색어 > ")` 후 keyword 입력
2. `repository.findByNameContaining(keyword)` 호출
3. 결과가 비어있으면 `view.printEmpty()`
4. 아니면 `view.printSampleList(samples)`

---

## 5. 테스트 계획

### 5-1. `SampleViewTest`

**테스트 방식**: `ByteArrayOutputStream`으로 `System.out` 캡처

| 케이스 | 검증 내용 |
|--------|---------|
| `printMenu()` | `[1]`, `[2]`, `[3]`, `[0]` 포함 확인 |
| `printSampleList(단일 항목)` | 헤더, 구분선, ID·시료명·단위(`min/ea`, `ea`) 포함 확인 |
| `printSampleList(복수 항목)` | 모든 행 출력 확인 |
| `printSampleList(빈 리스트)` | 헤더·구분선 출력 또는 빈 출력, 예외 없음 확인 |
| `printEmpty()` | "없습니다" 포함 확인 |
| `printSuccess("등록 완료")` | `[성공]` 포함 확인 |
| `printError("오류 메시지")` | `[오류]` 포함 확인 |
| `printPrompt("선택 > ")` | 해당 문자열 출력 확인 |

### 5-2. `SampleControllerTest`

**테스트 방식**: 실제 `SampleView` + 실제 `JsonSampleRepository`(`@TempDir`) + `ByteArrayOutputStream`으로 `System.out` 캡처  
(Mockito 미사용 — `@Spy` 불필요)

| 케이스 | 검증 내용 |
|--------|---------|
| `register()` 정상 흐름 | 입력 시뮬레이션 → `findAll()` 결과에 포함, `[성공]` 출력 |
| `register()` 중복 ID | 두 번째 등록 시 `[오류]` 출력, 목록 1건 유지 |
| `register()` 빈 이름 입력 (`""`) | `[오류]` 출력, 저장 안 됨 |
| `register()` 공백만 입력 이름 (`"   "`) | `[오류]` 출력, 저장 안 됨 |
| `register()` avgProductionTime 파싱 실패 (`"abc"`) | `[오류]` 출력, 저장 안 됨 |
| `register()` stock 파싱 실패 (`"xyz"`) | `[오류]` 출력, 저장 안 됨 |
| `register()` yield 범위 초과 (`1.5`) | `[오류]` 출력, 저장 안 됨 |
| `register()` yield 음수 (`-0.1`) | `[오류]` 출력, 저장 안 됨 |
| `register()` yield 경계값 `0.0` | 정상 저장 확인 |
| `register()` yield 경계값 `1.0` | 정상 저장 확인 |
| `register()` stock 음수 (`-1`) | `[오류]` 출력, 저장 안 됨 |
| `register()` avgProductionTime `0` | `[오류]` 출력, 저장 안 됨 |
| `listAll()` 빈 목록 | "없습니다" 포함 출력 |
| `listAll()` 항목 있음 | `[성공]` 없이 목록 출력 (테이블 헤더 포함) |
| `searchByName()` 일치 항목 있음 | 검색어 포함 시료만 출력 |
| `searchByName()` 일치 없음 | "없습니다" 포함 출력 |
| `searchByName()` 빈 키워드 | 전체 목록 출력 |

---

## 6. 완료 기준

- [ ] `./gradlew test` 전체 통과 (Phase 1 테스트 포함 회귀 없음)
- [ ] FR-SAM-01: 시료 등록 후 목록 조회 시 해당 시료 표시
- [ ] FR-SAM-02: 빈 목록/복수 항목 모두 출력 포맷 검증
- [ ] FR-SAM-03: 키워드 검색 — 일치/불일치/빈 키워드 세 경로 통과
- [ ] 잘못된 입력 오류 처리: 파싱 실패, 수율 범위 초과, 중복 ID, 빈 이름, 음수 재고, avgProductionTime <= 0
- [ ] yield 경계값 0.0/1.0 정상 등록 확인
