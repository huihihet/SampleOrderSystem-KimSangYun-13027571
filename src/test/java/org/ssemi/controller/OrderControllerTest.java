package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonProductionQueueRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.ProductionQueueRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.OrderView;
import org.ssemi.view.ProductionLineView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class OrderControllerTest {

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

    private OrderController ctrl(String input) {
        ProductionLineController prodLineCtrl = new ProductionLineController(
            sampleRepo, orderRepo, queueRepo, new ProductionLineView(), new Scanner(""));
        return new OrderController(sampleRepo, orderRepo, prodLineCtrl, new OrderView(), new Scanner(input));
    }

    // ── placeOrder 테스트 ──────────────────────────────────────────────

    @Test
    void placeOrder_정상_흐름_RESERVED_저장_성공_출력() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("1\n홍길동\n100\nY\n").placeOrder();

        assertTrue(output().contains("[성공]"));
        assertEquals(1, orderRepo.findAll().size());
        assertEquals(OrderStatus.RESERVED, orderRepo.findAll().get(0).getStatus());
    }

    @Test
    void placeOrder_번호_파싱_실패_오류_저장안됨() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("abc\n").placeOrder();

        assertTrue(output().contains("[오류]"));
        assertTrue(orderRepo.findAll().isEmpty());
    }

    @Test
    void placeOrder_번호_범위_초과_오류_저장안됨() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("99\n").placeOrder();

        assertTrue(output().contains("[오류]"));
        assertTrue(orderRepo.findAll().isEmpty());
    }

    @Test
    void placeOrder_빈_고객명_오류_저장안됨() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("1\n\n").placeOrder();

        assertTrue(output().contains("[오류]"));
        assertTrue(orderRepo.findAll().isEmpty());
    }

    @Test
    void placeOrder_수량_파싱_실패_오류_저장안됨() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("1\n홍길동\nabc\n").placeOrder();

        assertTrue(output().contains("[오류]"));
        assertTrue(orderRepo.findAll().isEmpty());
    }

    @Test
    void placeOrder_수량_0이하_오류_저장안됨() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("1\n홍길동\n0\n").placeOrder();

        assertTrue(output().contains("[오류]"));
        assertTrue(orderRepo.findAll().isEmpty());
    }

    @Test
    void placeOrder_orderId_자동생성_포맷_확인() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        outContent.reset();

        ctrl("1\n홍길동\n50\nY\n").placeOrder();

        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String expected = "ORD-" + today + "-0001";
        assertEquals(expected, orderRepo.findAll().get(0).getOrderId());
    }

    // ── approveOrder 테스트 ────────────────────────────────────────────

    @Test
    void approveOrder_RESERVED_없음_없습니다_출력() {
        ctrl("").approveOrder();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void approveOrder_재고_충분_CONFIRMED_재고_유지() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("1\n").approveOrder();

        assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("ORD-001").get().getStatus());
        // 재고 차감은 출고(ReleaseController) 시점에 수행 — 승인 후 재고 불변
        assertEquals(200, sampleRepo.findById("S-001").get().getStock());
        assertTrue(output().contains("CONFIRMED"));
    }

    @Test
    void approveOrder_재고_부족_PRODUCING_큐등록() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 50));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("1\n").approveOrder();

        assertEquals(OrderStatus.PRODUCING, orderRepo.findById("ORD-001").get().getStatus());
        assertFalse(queueRepo.findAll().isEmpty(), "큐에 항목 추가됨");
        assertTrue(output().contains("PRODUCING"));
    }

    @Test
    void approveOrder_재고_경계값_재고_equals_수량_CONFIRMED() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 100));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("1\n").approveOrder();

        assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("ORD-001").get().getStatus());
        // 재고 차감은 출고 시점에 수행 — 승인 후 재고 불변
        assertEquals(100, sampleRepo.findById("S-001").get().getStock());
    }

    @Test
    void approveOrder_잘못된_번호_오류_상태변경없음() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("abc\n").approveOrder();

        assertTrue(output().contains("[오류]"));
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    void approveOrder_범위_초과_번호_오류_상태변경없음() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("999\n").approveOrder();

        assertTrue(output().contains("[오류]"));
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-001").get().getStatus());
    }

    // ── rejectOrder 테스트 ────────────────────────────────────────────

    @Test
    void rejectOrder_정상_거절_REJECTED_성공_출력() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("1\n").rejectOrder();

        assertEquals(OrderStatus.REJECTED, orderRepo.findById("ORD-001").get().getStatus());
        assertTrue(output().contains("[성공]"));
    }

    @Test
    void rejectOrder_RESERVED_없음_없습니다_출력() {
        ctrl("").rejectOrder();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void rejectOrder_잘못된_번호_오류_상태변경없음() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        outContent.reset();

        ctrl("abc\n").rejectOrder();

        assertTrue(output().contains("[오류]"));
        assertEquals(OrderStatus.RESERVED, orderRepo.findById("ORD-001").get().getStatus());
    }
}
