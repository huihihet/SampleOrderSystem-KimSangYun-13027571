package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.view.ReleaseView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseControllerTest {

    @TempDir
    Path tempDir;

    private JsonSampleRepository sampleRepo;
    private JsonOrderRepository orderRepo;
    private ReleaseView realView;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        sampleRepo  = new JsonSampleRepository(tempDir.resolve("samples.json"));
        orderRepo   = new JsonOrderRepository(tempDir.resolve("orders.json"));
        outContent  = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
        realView    = new ReleaseView();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String output() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    private ReleaseController ctrl(String input) {
        return new ReleaseController(sampleRepo, orderRepo, realView,
                                     new Scanner(new StringReader(input)));
    }

    @Test
    void processRelease_CONFIRMED_없음_printEmpty() {
        ctrl("").processRelease();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void processRelease_재고_부족_CONFIRMED_목록에서_제외() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 10));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        // 재고(10) < 주문수량(100) → 출고 목록에서 제외 → 없습니다
        ctrl("").processRelease();

        assertTrue(output().contains("없습니다"));
        assertEquals(OrderStatus.CONFIRMED, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    void processRelease_정상_출고_RELEASE_전이() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("1\n").processRelease();

        assertEquals(OrderStatus.RELEASE, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    void processRelease_정상_출고_재고_차감() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("1\n").processRelease();

        assertEquals(100, sampleRepo.findById("S-001").get().getStock());
    }

    @Test
    void processRelease_정상_출고_성공_메시지() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("1\n").processRelease();

        assertTrue(output().contains("[성공]"));
    }

    @Test
    void processRelease_잘못된_번호_오류_메시지() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("abc\n").processRelease();

        assertTrue(output().contains("[오류]"));
    }

    @Test
    void processRelease_범위_초과_번호_오류_메시지() {
        sampleRepo.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 200));
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("99\n").processRelease();

        assertTrue(output().contains("[오류]"));
    }
}
