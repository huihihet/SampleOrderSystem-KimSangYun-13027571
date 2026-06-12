package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.SampleStatus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringViewTest {

    private MonitoringView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    private final Map<OrderStatus, Long> statusCounts = Map.of(
        OrderStatus.RESERVED,  2L,
        OrderStatus.PRODUCING, 1L,
        OrderStatus.CONFIRMED, 3L,
        OrderStatus.RELEASE,   1L
    );

    private final List<SampleStatus> sampleStatuses = List.of(
        new SampleStatus("S-001", "실리콘 웨이퍼", 120, "여유"),
        new SampleStatus("S-002", "GaAs 기판",       0, "고갈"),
        new SampleStatus("S-003", "SiC 기판",        15, "부족")
    );

    @BeforeEach
    void setUp() {
        view        = new MonitoringView();
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

    // ── printHeader 테스트 ──────────────────────────────────────────────

    @Test
    void printHeader_S_Semi_포함() {
        view.printHeader("12:00:00");
        assertTrue(output().contains("S-Semi") || output().contains("모니터링"));
    }

    @Test
    void printHeader_timestamp_포함() {
        view.printHeader("09:30:45");
        assertTrue(output().contains("09:30:45"));
    }

    @Test
    void printHeader_구분선_포함() {
        view.printHeader("12:00:00");
        assertTrue(output().contains("========"));
    }

    // ── printOrderSummary 테스트 ────────────────────────────────────────

    @Test
    void printOrderSummary_헤더_포함() {
        view.printOrderSummary(statusCounts);
        assertTrue(output().contains("[주문 현황]"));
    }

    @Test
    void printOrderSummary_RESERVED_건수_포함() {
        view.printOrderSummary(statusCounts);
        String out = output();
        assertTrue(out.contains("RESERVED") && out.contains("2"));
    }

    @Test
    void printOrderSummary_RESERVED_ANSI_blue() {
        view.printOrderSummary(statusCounts);
        assertTrue(output().contains("\033[34m") && output().contains("RESERVED"));
    }

    @Test
    void printOrderSummary_PRODUCING_ANSI_orange() {
        view.printOrderSummary(statusCounts);
        assertTrue(output().contains("\033[38;5;208m") && output().contains("PRODUCING"));
    }

    @Test
    void printOrderSummary_CONFIRMED_ANSI_green() {
        view.printOrderSummary(statusCounts);
        assertTrue(output().contains("\033[32m") && output().contains("CONFIRMED"));
    }

    @Test
    void printOrderSummary_RELEASE_ANSI_magenta() {
        view.printOrderSummary(statusCounts);
        assertTrue(output().contains("\033[35m") && output().contains("RELEASE"));
    }

    // ── printInventory 테스트 ───────────────────────────────────────────

    @Test
    void printInventory_재고현황_헤더_포함() {
        view.printInventory(sampleStatuses);
        assertTrue(output().contains("[시료별 재고 현황]"));
    }

    @Test
    void printInventory_여유_포함() {
        view.printInventory(sampleStatuses);
        assertTrue(output().contains("여유"));
    }

    @Test
    void printInventory_부족_ANSI_yellow() {
        view.printInventory(sampleStatuses);
        assertTrue(output().contains("\033[33m") && output().contains("부족"));
    }

    @Test
    void printInventory_고갈_ANSI_red() {
        view.printInventory(sampleStatuses);
        assertTrue(output().contains("\033[31m") && output().contains("고갈"));
    }
}
