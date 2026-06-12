package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.repository.JsonOrderRepository;
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

    private JsonOrderRepository orderRepo;
    private ReleaseView realView;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
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
        return new ReleaseController(orderRepo, realView, new Scanner(new StringReader(input)));
    }

    @Test
    void processRelease_CONFIRMED_없음_printEmpty() {
        ctrl("").processRelease();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void processRelease_정상_출고_RELEASE_전이() {
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("1\n").processRelease();

        assertEquals(OrderStatus.RELEASE, orderRepo.findById("ORD-001").get().getStatus());
    }

    @Test
    void processRelease_정상_출고_성공_메시지() {
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("1\n").processRelease();

        assertTrue(output().contains("[성공]"));
    }

    @Test
    void processRelease_잘못된_번호_오류_메시지() {
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("abc\n").processRelease();

        assertTrue(output().contains("[오류]"));
    }

    @Test
    void processRelease_범위_초과_번호_오류_메시지() {
        orderRepo.save(new Order("ORD-001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED));
        outContent.reset();

        ctrl("99\n").processRelease();

        assertTrue(output().contains("[오류]"));
    }
}
