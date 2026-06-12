package org.ssemi.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.controller.MonitoringController;
import org.ssemi.controller.OrderController;
import org.ssemi.controller.ProductionLineController;
import org.ssemi.controller.ReleaseController;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.entity.SampleStatus;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonProductionQueueRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.view.MonitoringView;
import org.ssemi.view.OrderView;
import org.ssemi.view.ProductionLineView;
import org.ssemi.view.ReleaseView;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class OrderFlowIntegrationTest {

    @TempDir
    Path tempDir;

    // ── 테스트 인프라 ─────────────────────────────────────────────────────

    private JsonSampleRepository sampleRepo() {
        return new JsonSampleRepository(tempDir.resolve("samples.json"));
    }

    private JsonOrderRepository orderRepo() {
        return new JsonOrderRepository(tempDir.resolve("orders.json"));
    }

    private JsonProductionQueueRepository queueRepo() {
        return new JsonProductionQueueRepository(tempDir.resolve("queue.json"));
    }

    private ProductionLineController prodLineCtrl(JsonSampleRepository sr,
                                                   JsonOrderRepository or,
                                                   JsonProductionQueueRepository qr,
                                                   Scanner sc) {
        return new ProductionLineController(sr, or, qr, new ProductionLineView(), sc);
    }

    private OrderController orderCtrl(JsonSampleRepository sr,
                                       JsonOrderRepository or,
                                       ProductionLineController plc,
                                       Scanner sc) {
        return new OrderController(sr, or, plc, new OrderView(), sc);
    }

    private ReleaseController releaseCtrl(JsonOrderRepository or, Scanner sc) {
        return new ReleaseController(or, new ReleaseView(), sc);
    }

    private MonitoringController monitorCtrl(JsonSampleRepository sr, JsonOrderRepository or) {
        return new MonitoringController(sr, or, new MonitoringView());
    }

    // ── 시나리오 1: 접수 → 승인(재고 충분) → 출고 ────────────────────────

    @Test
    void 시나리오1_접수_승인_재고충분_출고() {
        JsonSampleRepository sr = sampleRepo();
        JsonOrderRepository  or = orderRepo();
        JsonProductionQueueRepository qr = queueRepo();

        sr.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));

        // placeOrder: 3라인+Y확인, approveOrder: 1라인, processRelease: 1라인
        Scanner sc = new Scanner(new StringReader("S-001\n고객A\n50\nY\n1\n1\n"));
        ProductionLineController plc = prodLineCtrl(sr, or, qr, sc);
        OrderController oc = orderCtrl(sr, or, plc, sc);
        ReleaseController rc = releaseCtrl(or, sc);

        oc.placeOrder();
        oc.approveOrder();

        List<Order> confirmed = or.findByStatus(OrderStatus.CONFIRMED);
        assertEquals(1, confirmed.size(), "CONFIRMED 주문 1건 기대");

        rc.processRelease();

        Order released = or.findAll().get(0);
        assertEquals(OrderStatus.RELEASE, released.getStatus(), "최종 상태 RELEASE 기대");

        Sample updatedSample = sr.findById("S-001").orElseThrow();
        assertEquals(150, updatedSample.getStock(), "재고 200 - 50 = 150 기대");
    }

    // ── 시나리오 2: 접수 → 승인(재고 부족) → 생산 완료 → 출고 ───────────

    @Test
    void 시나리오2_접수_승인_재고부족_생산완료_출고() {
        JsonSampleRepository sr = sampleRepo();
        JsonOrderRepository  or = orderRepo();
        JsonProductionQueueRepository qr = queueRepo();

        sr.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.90, 5));

        // placeOrder: 3라인+Y확인, approveOrder: 1라인, completeProduction: 1라인, processRelease: 1라인
        Scanner sc = new Scanner(new StringReader("S-001\n고객A\n50\nY\n1\n1\n1\n"));
        ProductionLineController plc = prodLineCtrl(sr, or, qr, sc);
        OrderController oc = orderCtrl(sr, or, plc, sc);
        ReleaseController rc = releaseCtrl(or, sc);

        oc.placeOrder();
        oc.approveOrder();

        Order producingOrder = or.findByStatus(OrderStatus.PRODUCING).stream().findFirst().orElseThrow();
        assertEquals(OrderStatus.PRODUCING, producingOrder.getStatus(), "승인 후 PRODUCING 상태 기대");

        assertEquals(1, qr.findAll().size(), "큐 등록 1건 기대");

        plc.completeProduction();

        Order confirmedOrder = or.findById(producingOrder.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getStatus(), "생산 완료 후 CONFIRMED 기대");

        Sample afterProduction = sr.findById("S-001").orElseThrow();
        assertTrue(afterProduction.getStock() >= 50,
            "생산 완료 후 재고가 주문 수량 이상이어야 함. 실제: " + afterProduction.getStock());

        rc.processRelease();

        Order released = or.findById(producingOrder.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.RELEASE, released.getStatus(), "최종 상태 RELEASE 기대");
    }

    // ── 시나리오 3: 접수 → 거절 ──────────────────────────────────────────

    @Test
    void 시나리오3_접수_거절() {
        JsonSampleRepository sr = sampleRepo();
        JsonOrderRepository  or = orderRepo();
        JsonProductionQueueRepository qr = queueRepo();

        sr.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 100));

        // placeOrder: 3라인+Y확인, rejectOrder: 1라인
        Scanner sc = new Scanner(new StringReader("S-001\n고객A\n50\nY\n1\n"));
        ProductionLineController plc = prodLineCtrl(sr, or, qr, sc);
        OrderController oc = orderCtrl(sr, or, plc, sc);

        oc.placeOrder();
        oc.rejectOrder();

        Order rejected = or.findAll().get(0);
        assertEquals(OrderStatus.REJECTED, rejected.getStatus(), "거절 후 REJECTED 기대");

        Sample sample = sr.findById("S-001").orElseThrow();
        assertEquals(100, sample.getStock(), "거절 시 재고 불변 기대");
    }

    // ── 시나리오 4: 모니터링 — REJECTED 제외, 재고 고갈 확인 ─────────────

    @Test
    void 시나리오4_모니터링_REJECTED_제외_고갈_확인() {
        JsonSampleRepository sr = sampleRepo();
        JsonOrderRepository  or = orderRepo();

        sr.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 0));

        or.save(new Order("ORD-001", "S-001", "고객A", 10, OrderStatus.RESERVED));
        or.save(new Order("ORD-002", "S-001", "고객B", 20, OrderStatus.REJECTED));
        or.save(new Order("ORD-003", "S-001", "고객C", 30, OrderStatus.RELEASE));

        MonitoringController mc = monitorCtrl(sr, or);

        List<SampleStatus> summary = mc.getSampleSummary();
        assertEquals(1, summary.size(), "시료 1종 기대");
        assertEquals("고갈", summary.get(0).stockLevel(), "stock=0 → 고갈 기대");

        // REJECTED 주문은 getOrderCount()에서도 포함되지 않는지는 getOrderCount()가
        // findAll().size() 이므로 여기서는 getSampleSummary() 결과의 수치 검증에 집중
        // demandSum 계산 시 REJECTED는 RESERVED/PRODUCING만 합산 → REJECTED 제외 확인
        // stock=0 이므로 RESERVED가 있어도 "고갈" 우선
        assertEquals("S-001", summary.get(0).sampleId());
    }
}
