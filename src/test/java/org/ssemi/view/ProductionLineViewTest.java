package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.ProductionQueueItem;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductionLineViewTest {

    private ProductionLineView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        view = new ProductionLineView();
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

    private ProductionQueueItem makeItem(String queueId, String orderId, String sampleId,
                                          int required, int actual, int time) {
        int orderQty = required + 50;
        return new ProductionQueueItem(queueId, orderId, sampleId, orderQty, required, actual, time,
            "2026-06-12T10:00:00");
    }

    @Test
    void printMenu_항목_포함() {
        view.printMenu();
        String out = output();
        assertTrue(out.contains("[1]"));
        assertTrue(out.contains("[2]"));
        assertTrue(out.contains("[0]"));
    }

    @Test
    void printQueueList_단일_항목_헤더_및_내용_포함() {
        ProductionQueueItem item = makeItem("Q-001", "ORD-20260612-0001", "S-001", 50, 56, 1680);
        Map<String, String> names = Map.of("S-001", "실리콘 웨이퍼-8인치");
        view.printQueueList(List.of(item), names);
        String out = output();
        assertTrue(out.contains("ORD-20260612-0001"), "주문번호 포함");
        assertTrue(out.contains("실리콘 웨이퍼-8인치"), "시료명 포함");
        assertTrue(out.contains("56"), "실생산량 포함");
        assertTrue(out.contains("1680"), "예상완료 포함");
        // 헤더 확인
        assertTrue(out.contains("순서"), "헤더 포함");
        // 구분선 확인
        assertTrue(out.contains("---"), "구분선 포함");
    }

    @Test
    void printQueueList_복수_항목_행_수_및_하단주석_포함() {
        List<ProductionQueueItem> items = List.of(
            makeItem("Q-001", "ORD-20260612-0001", "S-001", 50, 56, 1680),
            makeItem("Q-002", "ORD-20260612-0002", "S-002", 50, 112, 8064)
        );
        Map<String, String> names = Map.of("S-001", "실리콘 웨이퍼", "S-002", "GaAs 기판");
        view.printQueueList(items, names);
        String out = output();
        assertTrue(out.contains("ORD-20260612-0001"));
        assertTrue(out.contains("ORD-20260612-0002"));
        assertTrue(out.contains("*"), "하단 주석 포함");
    }

    @Test
    void printQueueList_빈_리스트_헤더_구분선_출력_예외_없음() {
        assertDoesNotThrow(() -> view.printQueueList(List.of(), Map.of()));
        String out = output();
        assertTrue(out.contains("순서"), "헤더 포함");
        assertTrue(out.contains("---"), "구분선 포함");
    }

    @Test
    void printEmpty_없습니다_포함() {
        view.printEmpty();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void printSuccess_성공_태그_포함() {
        view.printSuccess("생산 완료");
        assertTrue(output().contains("[성공]"));
    }

    @Test
    void printError_오류_태그_포함() {
        view.printError("오류 메시지");
        assertTrue(output().contains("[오류]"));
    }
}
