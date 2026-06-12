package org.ssemi.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.controller.MonitoringController;
import org.ssemi.controller.OrderController;
import org.ssemi.controller.ProductionLineController;
import org.ssemi.controller.ReleaseController;
import org.ssemi.controller.SampleController;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonProductionQueueRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.view.MainView;
import org.ssemi.view.MonitoringView;
import org.ssemi.view.OrderView;
import org.ssemi.view.ProductionLineView;
import org.ssemi.view.ReleaseView;
import org.ssemi.view.SampleView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class RouterTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ── Spy 서브클래스 ────────────────────────────────────────────────────

    private static class SpySampleController extends SampleController {
        boolean handleSubMenuCalled = false;

        SpySampleController(Path dir) {
            super(new JsonSampleRepository(dir.resolve("samples.json")),
                  new SampleView(),
                  new Scanner(new StringReader("")));
        }

        @Override
        public void handleSubMenu() { handleSubMenuCalled = true; }
    }

    private static class SpyOrderController extends OrderController {
        boolean placeOrderCalled    = false;
        boolean handleSubMenuCalled = false;

        SpyOrderController(Path dir, ProductionLineController plc) {
            super(new JsonSampleRepository(dir.resolve("samples.json")),
                  new JsonOrderRepository(dir.resolve("orders.json")),
                  plc,
                  new OrderView(),
                  new Scanner(new StringReader("")));
        }

        @Override
        public void placeOrder()    { placeOrderCalled    = true; }

        @Override
        public void handleSubMenu() { handleSubMenuCalled = true; }
    }

    private static class SpyMonitoringController extends MonitoringController {
        boolean showMonitoringCalled = false;

        SpyMonitoringController(Path dir) {
            super(new JsonSampleRepository(dir.resolve("samples.json")),
                  new JsonOrderRepository(dir.resolve("orders.json")),
                  new MonitoringView());
        }

        @Override
        public void showMonitoring() { showMonitoringCalled = true; }

        @Override
        public int getSampleCount()   { return 3; }

        @Override
        public long getTotalStock()   { return 150L; }

        @Override
        public int getOrderCount()    { return 5; }
    }

    private static class SpyReleaseController extends ReleaseController {
        boolean processReleaseCalled = false;

        SpyReleaseController(Path dir) {
            super(new JsonOrderRepository(dir.resolve("orders.json")),
                  new ReleaseView(),
                  new Scanner(new StringReader("")));
        }

        @Override
        public void processRelease() { processReleaseCalled = true; }
    }

    private static class SpyProductionLineController extends ProductionLineController {
        boolean handleSubMenuCalled = false;

        SpyProductionLineController(Path dir) {
            super(new JsonSampleRepository(dir.resolve("samples.json")),
                  new JsonOrderRepository(dir.resolve("orders.json")),
                  new JsonProductionQueueRepository(dir.resolve("queue.json")),
                  new ProductionLineView(),
                  new Scanner(new StringReader("")));
        }

        @Override
        public void handleSubMenu()     { handleSubMenuCalled = true; }

        @Override
        public int getQueueWaitingCount() { return 2; }
    }

    // ── 헬퍼: Router 인스턴스 생성 ────────────────────────────────────────

    private Router buildRouter(SpySampleController sampleCtrl,
                                SpyOrderController orderCtrl,
                                SpyMonitoringController monitorCtrl,
                                SpyReleaseController releaseCtrl,
                                SpyProductionLineController prodCtrl) {
        return new Router(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl,
                          new MainView(), new Scanner(new StringReader("")));
    }

    // ── route() 테스트 ─────────────────────────────────────────────────────

    @Test
    void route_1_sampleController_handleSubMenu_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(1);

        assertTrue(sampleCtrl.handleSubMenuCalled);
    }

    @Test
    void route_2_orderController_placeOrder_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(2);

        assertTrue(orderCtrl.placeOrderCalled);
    }

    @Test
    void route_3_orderController_handleSubMenu_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(3);

        assertTrue(orderCtrl.handleSubMenuCalled);
    }

    @Test
    void route_4_monitoringController_showMonitoring_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(4);

        assertTrue(monitorCtrl.showMonitoringCalled);
    }

    @Test
    void route_5_productionLineController_handleSubMenu_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(5);

        assertTrue(prodCtrl.handleSubMenuCalled);
    }

    @Test
    void route_6_releaseController_processRelease_호출() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        router.route(6);

        assertTrue(releaseCtrl.processReleaseCalled);
    }

    @Test
    void route_0_false_반환() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        assertFalse(router.route(0));
    }

    @Test
    void route_99_default_true_반환() {
        SpySampleController sampleCtrl = new SpySampleController(tempDir);
        SpyProductionLineController prodCtrl = new SpyProductionLineController(tempDir);
        SpyOrderController orderCtrl = new SpyOrderController(tempDir, prodCtrl);
        SpyMonitoringController monitorCtrl = new SpyMonitoringController(tempDir);
        SpyReleaseController releaseCtrl = new SpyReleaseController(tempDir);

        Router router = buildRouter(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodCtrl);
        assertTrue(router.route(99));
    }
}
