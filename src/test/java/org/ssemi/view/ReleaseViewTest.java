package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseViewTest {

    private ReleaseView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        view        = new ReleaseView();
        outContent  = new ByteArrayOutputStream();
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

    @Test
    void printOrderList_헤더_포함() {
        Order o = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED);
        view.printOrderList(List.of(o));
        assertTrue(output().contains("주문번호"));
    }

    @Test
    void printOrderList_구분선_포함() {
        Order o = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED);
        view.printOrderList(List.of(o));
        assertTrue(output().contains("---"));
    }

    @Test
    void printOrderList_orderId_포함() {
        Order o = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED);
        view.printOrderList(List.of(o));
        assertTrue(output().contains("ORD-20260612-0001"));
    }

    @Test
    void printOrderList_빈_리스트_예외_없음() {
        assertDoesNotThrow(() -> view.printOrderList(List.of()));
    }

    @Test
    void printEmpty_메시지_포함() {
        view.printEmpty();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void printSuccess_성공_태그_포함() {
        view.printSuccess("출고 완료: ORD-001");
        assertTrue(output().contains("[성공]"));
    }

    @Test
    void printError_오류_태그_포함() {
        view.printError("올바른 번호를 입력해 주세요.");
        assertTrue(output().contains("[오류]"));
    }
}
