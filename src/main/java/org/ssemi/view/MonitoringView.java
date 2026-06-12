package org.ssemi.view;

import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.SampleStatus;

import java.util.List;
import java.util.Map;

public class MonitoringView {

    private static final String ANSI_RESET   = "\033[0m";
    private static final String ANSI_YELLOW  = "\033[33m";
    private static final String ANSI_RED     = "\033[31m";
    private static final String ANSI_BLUE    = "\033[34m";
    private static final String ANSI_ORANGE  = "\033[38;5;208m";
    private static final String ANSI_GREEN   = "\033[32m";
    private static final String ANSI_MAGENTA = "\033[35m";

    public void render(Map<OrderStatus, Long> statusCounts,
                       List<SampleStatus> sampleStatuses,
                       String timestamp) {
        clearScreen();
        printHeader(timestamp);
        printOrderSummary(statusCounts);
        printInventory(sampleStatuses);
    }

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printHeader(String timestamp) {
        System.out.println("=".repeat(40));
        System.out.println("  S-Semi 생산주문관리 — 모니터링");
        System.out.println("  마지막 갱신: " + timestamp);
        System.out.println("=".repeat(40));
    }

    public void printOrderSummary(Map<OrderStatus, Long> statusCounts) {
        System.out.println("[주문 현황]");
        System.out.printf("  %sRESERVED%s   :  %d건%n",
            ANSI_BLUE, ANSI_RESET, statusCounts.getOrDefault(OrderStatus.RESERVED, 0L));
        System.out.printf("  %sPRODUCING%s  :  %d건%n",
            ANSI_ORANGE, ANSI_RESET, statusCounts.getOrDefault(OrderStatus.PRODUCING, 0L));
        System.out.printf("  %sCONFIRMED%s  :  %d건%n",
            ANSI_GREEN, ANSI_RESET, statusCounts.getOrDefault(OrderStatus.CONFIRMED, 0L));
        System.out.printf("  %sRELEASE%s    :  %d건%n",
            ANSI_MAGENTA, ANSI_RESET, statusCounts.getOrDefault(OrderStatus.RELEASE, 0L));
    }

    public void printInventory(List<SampleStatus> sampleStatuses) {
        System.out.println("[시료별 재고 현황]");
        System.out.println("  ID       이름           재고      상태");
        System.out.println("  -------- ------------ -------- ------");
        for (SampleStatus s : sampleStatuses) {
            System.out.printf("  %-8s  %-12s  %7d  %s%n",
                s.sampleId(), s.name(), s.stock(), colorize(s.stockLevel()));
        }
    }

    public void printExitHint() {
        System.out.println();
        System.out.println("  [ Enter 키를 누르면 메인 메뉴로 돌아갑니다 / 3초마다 자동 갱신 ]");
    }

    private String colorize(String stockLevel) {
        return switch (stockLevel) {
            case "부족" -> ANSI_YELLOW + "부족" + ANSI_RESET;
            case "고갈" -> ANSI_RED    + "고갈" + ANSI_RESET;
            default     -> stockLevel;
        };
    }
}
