package org.ssemi.view;

import org.ssemi.model.entity.ProductionQueueItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ProductionLineView {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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
        System.out.println("=".repeat(72));
        System.out.printf("  생산 현황  (%s 기준)%n",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println("=".repeat(72));
        System.out.println(
            ViewUtil.padRight("순서", 5) +
            ViewUtil.padRight("주문번호", 22) +
            ViewUtil.padRight("시료명", 16) +
            ViewUtil.padRight("목표량", 9) +
            ViewUtil.padRight("잔여량", 9) +
            ViewUtil.padRight("완료시각", 12) +
            "남은시간");
        System.out.println("-".repeat(72));
        for (int i = 0; i < items.size(); i++) {
            ProductionQueueItem item = items.get(i);
            String sampleName   = sampleNames.getOrDefault(item.getSampleId(), item.getSampleId());
            boolean waiting     = isWaiting(item);
            int     remaining   = waiting ? item.getActualProductionQuantity() : calcRemainingUnits(item);
            String  expectedTime = calcExpectedTime(item.getEnqueuedAt(), item.getTotalProductionTime(),
                                                    item.getActualProductionQuantity());
            String  timeLeft    = waiting ? "(대기)"
                                : calcTimeLeft(item.getEnqueuedAt(), item.getTotalProductionTime(),
                                               item.getActualProductionQuantity());
            System.out.println(
                ViewUtil.padRight(String.valueOf(i + 1), 5) +
                ViewUtil.padRight(item.getOrderId(), 22) +
                ViewUtil.padRight(sampleName, 16) +
                ViewUtil.padRight(item.getActualProductionQuantity() + " ea", 9) +
                ViewUtil.padRight(remaining + " ea", 9) +
                ViewUtil.padRight(expectedTime, 12) +
                timeLeft);
        }
        System.out.println("-".repeat(72));
        System.out.println("* 단위당 " + (items.isEmpty() ? "-" :
            items.get(0).getTotalProductionTime()) + "초/ea  |  1초마다 자동 갱신");
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

    // 아직 시작하지 않은 항목(FIFO 대기) 여부
    private boolean isWaiting(ProductionQueueItem item) {
        try {
            return LocalDateTime.parse(item.getEnqueuedAt()).isAfter(LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    // 잔여 생산 수량: 경과 초 / 단위당 초수 = 완료된 수량 → 목표 - 완료
    private int calcRemainingUnits(ProductionQueueItem item) {
        try {
            long elapsed   = ChronoUnit.SECONDS.between(
                LocalDateTime.parse(item.getEnqueuedAt()), LocalDateTime.now());
            int  secsEach  = item.getTotalProductionTime();   // 단위당 초수
            int  total     = item.getActualProductionQuantity();
            if (elapsed <= 0 || secsEach <= 0) return total;
            int done = (int) Math.min(elapsed / secsEach, total);
            return total - done;
        } catch (Exception e) {
            return item.getActualProductionQuantity();
        }
    }

    // 예상 완료 시각 = enqueuedAt + (단위당 초 × 총 수량)
    private String calcExpectedTime(String enqueuedAt, int secsEach, int totalQty) {
        try {
            return LocalDateTime.parse(enqueuedAt)
                .plusSeconds((long) secsEach * totalQty)
                .format(TIME_FMT);
        } catch (Exception e) {
            return "??:??";
        }
    }

    // 남은 시간 = 완료 예정 - now
    private String calcTimeLeft(String enqueuedAt, int secsEach, int totalQty) {
        try {
            LocalDateTime expected = LocalDateTime.parse(enqueuedAt)
                .plusSeconds((long) secsEach * totalQty);
            long secs = ChronoUnit.SECONDS.between(LocalDateTime.now(), expected);
            if (secs <= 0) return "(완료 대기)";
            if (secs < 60) return String.format("(%ds)", secs);
            return String.format("(%dm %ds)", secs / 60, secs % 60);
        } catch (Exception e) {
            return "";
        }
    }

}
