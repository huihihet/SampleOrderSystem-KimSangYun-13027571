package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.ProductionQueueItem;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonProductionQueueRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.ProductionQueueRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.ProductionLineView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ProductionLineControllerTest {

    @TempDir
    Path tempDir;

    private SampleRepository sampleRepo;
    private OrderRepository orderRepo;
    private ProductionQueueRepository queueRepo;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        sampleRepo = new JsonSampleRepository(tempDir.resolve("samples.json"));
        orderRepo  = new JsonOrderRepository(tempDir.resolve("orders.json"));
        queueRepo  = new JsonProductionQueueRepository(tempDir.resolve("queue.json"));
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String output() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    private ProductionLineController ctrl(String input) {
        return new ProductionLineController(
            sampleRepo, orderRepo, queueRepo, new ProductionLineView(), new Scanner(input));
    }

    // ── registerProductionQueue 테스트 ──────────────────────────────────

    @Test
    void registerProductionQueue_수율1_부족10_실생산량12() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        ProductionQueueItem item = queueRepo.findAll().get(0);
        // 부족분 = 100 - 90 = 10, ceil(10 / (1.0 * 0.9)) = ceil(11.11) = 12
        assertEquals(10, item.getRequiredQuantity());
        assertEquals(12, item.getActualProductionQuantity());
        assertEquals(1, queueRepo.findAll().size());
    }

    @Test
    void registerProductionQueue_수율05_부족10_실생산량23() {
        Sample sample = new Sample("S-001", "GaAs 기판", 72, 0.5, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "김철수", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        ProductionQueueItem item = queueRepo.findAll().get(0);
        // 부족분 = 10, ceil(10 / (0.5 * 0.9)) = ceil(10 / 0.45) = ceil(22.22) = 23
        assertEquals(23, item.getActualProductionQuantity());
    }

    @Test
    void registerProductionQueue_수율1_부족9_실생산량10() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 91);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        ProductionQueueItem item = queueRepo.findAll().get(0);
        // 부족분 = 100 - 91 = 9, ceil(9 / 0.9) = 10 (딱 떨어짐)
        assertEquals(9, item.getRequiredQuantity());
        assertEquals(10, item.getActualProductionQuantity());
    }

    @Test
    void registerProductionQueue_총생산시간_단위당_초수_equals_avgProductionTime() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        ProductionQueueItem item = queueRepo.findAll().get(0);
        // totalProductionTime = 단위당 초수 = avgProductionTime
        assertEquals(30, item.getTotalProductionTime());
    }

    @Test
    void registerProductionQueue_queueId_포맷_Q001() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        ProductionQueueItem item = queueRepo.findAll().get(0);
        assertEquals("Q-001", item.getQueueId());
    }

    @Test
    void registerProductionQueue_수율0_오류_출력_큐_항목_없음() {
        Sample sample = new Sample("S-001", "수율0시료", 30, 0.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);

        ctrl("").registerProductionQueue(order, sample);

        assertTrue(output().contains("[오류]"), "오류 메시지 출력");
        assertTrue(queueRepo.findAll().isEmpty(), "큐에 항목 추가 안 됨");
    }

    // ── showQueue 테스트 ───────────────────────────────────────────────

    @Test
    void showQueue_빈_큐_없습니다_출력() {
        ctrl("").showQueue();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void showQueue_항목_있음_시료명_포함_출력() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);
        ctrl("").registerProductionQueue(order, sample);
        outContent.reset();

        ctrl("").showQueue();

        assertTrue(output().contains("실리콘 웨이퍼"), "시료명 포함");
    }

    // ── completeProduction 테스트 ──────────────────────────────────────

    @Test
    void completeProduction_정상_완료_재고증가_CONFIRMED_큐삭제() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);
        ctrl("").registerProductionQueue(order, sample);
        int actualQty = queueRepo.findAll().get(0).getActualProductionQuantity();
        outContent.reset();

        ctrl("1\n").completeProduction();

        // 재고 증가 확인
        int newStock = sampleRepo.findById("S-001").get().getStock();
        assertEquals(90 + actualQty, newStock);
        // 주문 CONFIRMED 전이
        assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("ORD-001").get().getStatus());
        // 큐 항목 삭제
        assertTrue(queueRepo.findAll().isEmpty());
        assertTrue(output().contains("[성공]"));
    }

    @Test
    void completeProduction_빈_큐_없습니다_출력_반환() {
        ctrl("").completeProduction();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void completeProduction_잘못된_번호_오류_큐_변경없음() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);
        ctrl("").registerProductionQueue(order, sample);
        outContent.reset();

        ctrl("abc\n").completeProduction();

        assertTrue(output().contains("[오류]"));
        assertFalse(queueRepo.findAll().isEmpty(), "큐 항목 삭제 안 됨");
    }

    @Test
    void completeProduction_범위_초과_번호_오류_출력() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 1.0, 90);
        sampleRepo.save(sample);
        Order order = new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        orderRepo.save(order);
        ctrl("").registerProductionQueue(order, sample);
        outContent.reset();

        ctrl("999\n").completeProduction();

        assertTrue(output().contains("[오류]"));
        assertFalse(queueRepo.findAll().isEmpty(), "큐 항목 삭제 안 됨");
    }
}
