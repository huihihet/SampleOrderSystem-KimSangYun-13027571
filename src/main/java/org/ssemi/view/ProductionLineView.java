package org.ssemi.view;

import org.ssemi.model.entity.ProductionQueueItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ProductionLineView {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printMenu() {
        clearScreen();
        System.out.println("=".repeat(63));
        System.out.println("  생산 라인");
        System.out.println("=".repeat(63));
        System.out.println("[1] 현황 조회 (자동 갱신)");
        System.out.println("[2] 생산 완료 처리");
        System.out.println("[0] 뒤로");
        System.out.print("선택 > ");
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printQueueList(List<ProductionQueueItem> items, Map<String, String> sampleNames) {
        clearScreen();
        System.out.println("=".repeat(63));
        System.out.printf("  생산 현황  (%s 기준)%n", LocalDateTime.now().format(TIME_FMT));
        System.out.println("=".repeat(63));
        System.out.printf("%-6s %-22s %-18s %-9s %-9s %-10s %s%n",
            "순서", "주문번호", "시료명", "주문량", "부족분", "실생산량", "예상완료");
        System.out.println("-".repeat(88));
        for (int i = 0; i < items.size(); i++) {
            ProductionQueueItem item = items.get(i);
            String sampleName = sampleNames.getOrDefault(item.getSampleId(), item.getSampleId());
            String expectedTime = calcExpectedTime(item.getEnqueuedAt(), item.getTotalProductionTime());
            String remaining   = calcRemaining(item.getEnqueuedAt(), item.getTotalProductionTime());
            System.out.printf("%-6d %-22s %-18s %-9s %-9s %-10s %s %s%n",
                i + 1,
                item.getOrderId(),
                sampleName,
                item.getOrderQuantity() + " ea",
                item.getRequiredQuantity() + " ea",
                item.getActualProductionQuantity() + " ea",
                expectedTime,
                remaining);
        }
        System.out.println("-".repeat(88));
        System.out.println("* 부족분 = 주문량 - 재고, 실생산량 = ceil(부족분 / (수율 × 0.9))");
    }

    public void printExitHint() {
        System.out.println();
        System.out.println("  [ Enter 키를 누르면 메뉴로 돌아갑니다 / 3초마다 자동 갱신 ]");
    }

    public void printSuccess(String message) {
        System.out.println("[성공] " + message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printEmpty() {
        clearScreen();
        System.out.println("  생산 중인 항목이 없습니다.");
    }

    public void printPause() {
        System.out.println();
        System.out.print("  [ Enter 키로 계속 ]  ");
    }

    private String calcExpectedTime(String enqueuedAt, int totalMinutes) {
        try {
            LocalDateTime enqueued = LocalDateTime.parse(enqueuedAt);
            return enqueued.plusMinutes(totalMinutes).format(TIME_FMT);
        } catch (Exception e) {
            return "??:??";
        }
    }

    private String calcRemaining(String enqueuedAt, int totalMinutes) {
        try {
            LocalDateTime expected = LocalDateTime.parse(enqueuedAt).plusMinutes(totalMinutes);
            long mins = ChronoUnit.MINUTES.between(LocalDateTime.now(), expected);
            if (mins <= 0) return "(완료 대기)";
            return String.format("(%dh %dm 남음)", mins / 60, mins % 60);
        } catch (Exception e) {
            return "";
        }
    }
}
