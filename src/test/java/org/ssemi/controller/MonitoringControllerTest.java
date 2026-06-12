package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.entity.SampleStatus;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.MonitoringView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringControllerTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent  = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ── 테스트 인프라 ────────────────────────────────────────────────────

    private static class SpyMonitoringView extends MonitoringView {
        Map<OrderStatus, Long> capturedStatusCounts;
        List<SampleStatus> capturedSampleStatuses;
        int renderCallCount = 0;

        @Override
        public void render(Map<OrderStatus, Long> statusCounts,
                           List<SampleStatus> sampleStatuses,
                           String timestamp) {
            this.capturedStatusCounts   = statusCounts;
            this.capturedSampleStatuses = sampleStatuses;
            this.renderCallCount++;
        }

        @Override
        public void clearScreen() {}
    }

    private static class StubSampleRepository implements SampleRepository {
        private final List<Sample> samples;

        StubSampleRepository(Sample... samples) {
            this.samples = List.of(samples);
        }

        @Override public List<Sample> findAll() { return samples; }
        @Override public Optional<Sample> findById(String id) { return Optional.empty(); }
        @Override public List<Sample> findByNameContaining(String keyword) { return List.of(); }
        @Override public void save(Sample sample) {}
        @Override public void update(Sample sample) {}
        @Override public void deleteById(String sampleId) {}
    }

    private static class StubOrderRepository implements OrderRepository {
        private final List<Order> orders;

        StubOrderRepository(Order... orders) {
            this.orders = List.of(orders);
        }

        @Override public List<Order> findAll() { return orders; }
        @Override public List<Order> findByStatus(OrderStatus s) {
            return orders.stream().filter(o -> o.getStatus() == s).collect(Collectors.toList());
        }
        @Override public List<Order> findBySampleId(String id) { return List.of(); }
        @Override public Optional<Order> findById(String orderId) { return Optional.empty(); }
        @Override public void save(Order order) {}
        @Override public void update(Order order) {}
    }

    // ── showMonitoring 테스트 ───────────────────────────────────────────

    @Test
    void showMonitoring_render_1회_호출() {
        SpyMonitoringView spy = new SpyMonitoringView();
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(),
            new StubOrderRepository(),
            spy
        );
        ctrl.showMonitoring();
        assertEquals(1, spy.renderCallCount);
    }

    @Test
    void showMonitoring_REJECTED_제외_statusCounts() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Order rejected = new Order("O-001", "S-001", "홍길동", 10, OrderStatus.REJECTED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(),
            new StubOrderRepository(rejected),
            spy
        );
        ctrl.showMonitoring();
        assertFalse(spy.capturedStatusCounts.containsKey(OrderStatus.REJECTED));
    }

    @Test
    void showMonitoring_RESERVED_건수() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Order o1 = new Order("O-001", "S-001", "홍길동",  10, OrderStatus.RESERVED);
        Order o2 = new Order("O-002", "S-001", "이순신",  20, OrderStatus.RESERVED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(),
            new StubOrderRepository(o1, o2),
            spy
        );
        ctrl.showMonitoring();
        assertEquals(2L, spy.capturedStatusCounts.get(OrderStatus.RESERVED));
    }

    @Test
    void showMonitoring_CONFIRMED_포함_demandSum() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 10);
        // 재고 차감은 출고 시점: CONFIRMED는 재고 미차감 상태이므로 demand에 포함
        // stock=10 < demand=100 → 부족
        Order confirmed = new Order("O-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(confirmed),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("부족", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    @Test
    void showMonitoring_RELEASE_제외_demandSum() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 10);
        // RELEASE 주문만 있으면 demandSum=0, stock=10 >= 0 → 여유
        Order released = new Order("O-001", "S-001", "홍길동", 100, OrderStatus.RELEASE);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(released),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("여유", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    // ── calcStockLevel 간접 테스트 ──────────────────────────────────────

    @Test
    void calcStockLevel_stock_0_고갈() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 0);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("고갈", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    @Test
    void calcStockLevel_stock_lt_demand_부족() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 5);
        Order reserved = new Order("O-001", "S-001", "홍길동", 10, OrderStatus.RESERVED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(reserved),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("부족", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    @Test
    void calcStockLevel_stock_eq_demand_여유() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 50);
        Order reserved = new Order("O-001", "S-001", "홍길동", 50, OrderStatus.RESERVED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(reserved),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("여유", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    @Test
    void calcStockLevel_stock_gt_demand_여유() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 100);
        Order reserved = new Order("O-001", "S-001", "홍길동", 50, OrderStatus.RESERVED);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(reserved),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("여유", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    @Test
    void calcStockLevel_RELEASE만있으면_demandSum_0_여유() {
        SpyMonitoringView spy = new SpyMonitoringView();
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 10);
        // RELEASE 주문만 있으면 demandSum=0, stock=10 → 여유
        Order released = new Order("O-001", "S-001", "홍길동", 100, OrderStatus.RELEASE);
        MonitoringController ctrl = new MonitoringController(
            new StubSampleRepository(sample),
            new StubOrderRepository(released),
            spy
        );
        ctrl.showMonitoring();
        assertEquals("여유", spy.capturedSampleStatuses.get(0).stockLevel());
    }

    // ── getSampleSummary 테스트 ─────────────────────────────────────────

    @Test
    void getSampleSummary_시료수_일치() {
        JsonSampleRepository sampleRepo = new JsonSampleRepository(tempDir.resolve("samples.json"));
        JsonOrderRepository  orderRepo  = new JsonOrderRepository(tempDir.resolve("orders.json"));
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 100));
        sampleRepo.save(new Sample("S-002", "GaAs 기판",     60, 0.85,  50));

        SpyMonitoringView spy = new SpyMonitoringView();
        MonitoringController ctrl = new MonitoringController(sampleRepo, orderRepo, spy);

        List<SampleStatus> result = ctrl.getSampleSummary();
        assertEquals(2, result.size());
    }

    @Test
    void getSampleSummary_render_미호출() {
        JsonSampleRepository sampleRepo = new JsonSampleRepository(tempDir.resolve("samples.json"));
        JsonOrderRepository  orderRepo  = new JsonOrderRepository(tempDir.resolve("orders.json"));

        SpyMonitoringView spy = new SpyMonitoringView();
        MonitoringController ctrl = new MonitoringController(sampleRepo, orderRepo, spy);

        ctrl.getSampleSummary();
        assertEquals(0, spy.renderCallCount);
    }
}
