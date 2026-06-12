package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OrderViewTest {

    private OrderView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        view = new OrderView();
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

    @Test
    void printApprovalMenu_항목_포함() {
        view.printApprovalMenu();
        String out = output();
        assertTrue(out.contains("[1]"));
        assertTrue(out.contains("[2]"));
        assertTrue(out.contains("[0]"));
    }

    @Test
    void printOrderList_단일_항목_헤더_및_내용_포함() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        Map<String, String> names = Map.of("S-001", "실리콘 웨이퍼-8인치");
        view.printOrderList(List.of(order), names);
        String out = output();
        assertTrue(out.contains("ORD-20260612-0001"), "주문번호 포함");
        assertTrue(out.contains("실리콘 웨이퍼-8인치"), "시료명 포함");
        assertTrue(out.contains("홍길동"), "고객명 포함");
        assertTrue(out.contains("100"), "수량 포함");
        assertTrue(out.contains("RESERVED"), "상태 포함");
        // 헤더 확인
        assertTrue(out.contains("번호"), "헤더 포함");
        // 구분선 확인
        assertTrue(out.contains("---"), "구분선 포함");
    }

    @Test
    void printOrderList_복수_항목_행_수_일치() {
        List<Order> orders = List.of(
            new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED),
            new Order("ORD-20260612-0002", "S-002", "김철수", 50, OrderStatus.PRODUCING)
        );
        Map<String, String> names = Map.of("S-001", "실리콘 웨이퍼", "S-002", "GaAs 기판");
        view.printOrderList(orders, names);
        String out = output();
        assertTrue(out.contains("ORD-20260612-0001"));
        assertTrue(out.contains("ORD-20260612-0002"));
    }

    @Test
    void printOrderList_빈_리스트_헤더_구분선_출력_예외_없음() {
        assertDoesNotThrow(() -> view.printOrderList(List.of(), Map.of()));
        String out = output();
        assertTrue(out.contains("번호"), "헤더 포함");
        assertTrue(out.contains("---"), "구분선 포함");
    }

    @Test
    void printApprovalDetail_재고_충분_CONFIRMED_포함_실생산량_없음() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 200);
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.CONFIRMED);
        view.printApprovalDetail(sample, order, 0, 0, 0);
        String out = output();
        assertTrue(out.contains("CONFIRMED"), "CONFIRMED 포함");
        assertFalse(out.contains("실생산량"), "실생산량 행 없음");
    }

    @Test
    void printApprovalDetail_재고_부족_PRODUCING_및_실생산량_포함() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 50);
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.PRODUCING);
        view.printApprovalDetail(sample, order, 50, 56, 1680);
        String out = output();
        assertTrue(out.contains("PRODUCING"), "PRODUCING 포함");
        assertTrue(out.contains("실생산량"), "실생산량 행 포함");
        assertTrue(out.contains("56"), "실생산량 수치 포함");
        assertTrue(out.contains("1680"), "생산시간 포함");
    }

    @Test
    void printEmpty_없습니다_포함() {
        view.printEmpty();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void printSuccess_성공_태그_포함() {
        view.printSuccess("테스트 메시지");
        assertTrue(output().contains("[성공]"));
    }

    @Test
    void printError_오류_태그_포함() {
        view.printError("오류 메시지");
        assertTrue(output().contains("[오류]"));
    }
}
