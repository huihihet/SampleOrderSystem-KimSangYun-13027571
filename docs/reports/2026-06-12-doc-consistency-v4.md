# 문서 정합성 검증 보고서

**일시**: 2026-06-12  
**검증 문서**:
- `SampleOrderSystem/CLAUDE.md`
- `docs/PRD.md` (v1.1.0)
- `docs/PLAN.md` (v1.1.0)

**이전 보고서**: `docs/reports/2026-06-12-doc-consistency-v3.md` (문제 5건: CRITICAL 2, WARNING 3)  
**결과**: 문제 3건 발견 (CRITICAL: 0, WARNING: 2, INFO: 1)

---

## v3 대비 해소된 문제

| 이전 번호 | 내용 | 상태 |
|----------|------|------|
| CRITICAL-1 | Router 메뉴 번호 ↔ MainView 출력 불일치 | 해소됨 — PRD·PLAN 모두 6개 메뉴 체계로 통일 |
| CRITICAL-2 (부분) | SampleFixture `S001` → `S-001` 형식 | 해소됨 |
| CRITICAL-2 (부분) | OrderFixture `O001` → `ORD-YYYYMMDD-NNNN` 형식 | 해소됨 |
| WARNING-1 | PRD Section 3.1 ↔ Section 8.2 메뉴 내부 모순 | 해소됨 — Section 3.1이 6개 메뉴로 업데이트됨 |
| WARNING-2 | PRD Section 2.4 `requiredQuantity` 설명 모호 | 해소됨 — "주문 수량 - 재고, 항상 양수"로 명확화됨 |
| WARNING-3 | CLAUDE.md 현재 상태 표 `PLAN.md` 항목 "미작성" 잔류 | 해소됨 — "작성 완료 (v1.1.0)"으로 업데이트됨 |

---

## 발견된 문제

### [WARNING-1] 큐 ID 형식 충돌 — PLAN.md vs PRD.md

- **위치**:
  - `docs/PRD.md` — Section 8.1 ID 형식 테이블
  - `docs/PLAN.md` — Phase 1 `ProductionQueueItem` Entity 설계 (queueId 필드 주석)
  - `docs/PLAN.md` — Phase 3 `ProductionLineController.createQueueItem()` 설계
- **설명**:
  PRD.md Section 8.1에서 큐 ID 형식을 `Q-NNN` (예: `Q-001`, 하이픈 포함)으로 확정하였으나,
  PLAN.md 두 곳에서 여전히 하이픈 없는 형식을 사용한다.

  | 위치 | 기재 내용 |
  |------|---------|
  | PRD.md Section 8.1 | `Q-NNN` (예: `Q-001`) |
  | PLAN.md Phase 1 Entity `ProductionQueueItem` queueId 주석 | `"Q001" 포맷` |
  | PLAN.md Phase 3 `createQueueItem()` queueId 생성 코드 | `"Q" + String.format("%03d", ...)` → `Q001` |

- **권장 조치**: PLAN.md Phase 1 Entity 설계의 queueId 주석을 `"Q-001" 포맷`으로 수정하고, Phase 3 `createQueueItem()` 코드 예시를 `"Q-" + String.format("%03d", ...)`로 수정한다.

---

### [WARNING-2] CLAUDE.md Router.java 메뉴 개수 기재 오류

- **위치**: `SampleOrderSystem/CLAUDE.md` — "PoC 이식 상세 계획 > ConsoleMVC 이식" 테이블
- **설명**:
  CLAUDE.md ConsoleMVC 이식 표의 `app/Router.java` 변경 사항이 `"5개 메뉴로 확장, 복수 Controller 주입"`으로 기재되어 있다.
  그러나 PRD Section 3.1 및 PLAN.md Phase 5에서 메뉴 체계가 6개(`[1]~[6] + [0]`)로 확정되었으므로 숫자가 맞지 않는다.
- **권장 조치**: CLAUDE.md의 해당 셀을 `"6개 메뉴로 확장, 복수 Controller 주입"`으로 수정한다.

---

### [INFO-1] PLAN.md 테스트 계획 내 "5개 메뉴" 언급 미동기화

- **위치**: `docs/PLAN.md` — Phase 5 테스트 계획 테이블
- **설명**:
  Phase 5 테스트 계획 테이블의 두 항목에서 메뉴 개수가 구버전(5개) 기준으로 남아있다.

  | 테스트 파일 | 현재 기재 | 올바른 내용 |
  |-----------|---------|-----------|
  | `RouterTest` | `5개 메뉴 각각 올바른 Controller 호출, menu=0 종료` | `6개 메뉴 각각 올바른 Controller 호출, menu=0 종료` |
  | `MainViewTest` | `시료 요약 포함, 5개 메뉴 항목` | `시료 요약 포함, 6개 메뉴 항목` |

  구현 오류를 직접 유발하지는 않으나, 테스트 케이스 작성 시 검증 범위를 5개로 오해할 수 있다.
- **권장 조치**: 두 항목의 `5개 메뉴`를 `6개 메뉴`로 수정한다.

---

## 통과 항목

- **[A] 교차 참조**: 이상 없음 — PLAN.md의 `[PRD.md](PRD.md)` 링크가 실제 파일과 일치. Phase 별도 파일 없음.
- **[B] 기술 스택**: 이상 없음 — Java 17+, Gradle 8.x, Gson 2.11.0, JUnit Jupiter 6.0.0, JaCoCo 세 문서 모두 일치.
- **[C] 설계 제약 반영**: WARNING 2건 발견 (위 상세 참조). MVC 레이어 위반, 수정 금지 제약 위반 없음.
- **[D] 완료 기준**: 이상 없음 — Phase 0~5 전체에 완료 기준 명시됨.
- **[E] 내부 모순**: 이상 없음 — PRD 내부(Section 3.1 ↔ 8.2) 모순 해소됨.
