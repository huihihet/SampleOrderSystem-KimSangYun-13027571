package org.ssemi.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainView {

    private static final String LINE = "=".repeat(63);
    private static final DateTimeFormatter TIMESTAMP_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printMainMenu(int totalSamples, long totalStock, long totalOrders, long prodLineWaiting) {
        clearScreen();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        System.out.println(LINE);
        System.out.println("반도체 시료 생산주문관리 시스템");
        System.out.println("시스템 현황  " + timestamp);
        System.out.println();
        System.out.printf("등록 시료 |%d종    총 재고    %s ea%n",
            totalSamples, String.format("%,d", totalStock));
        System.out.printf("전체 주문  %d건    생산라인   %d건 대기%n",
            totalOrders, prodLineWaiting);
        System.out.println();
        System.out.println("[1] 시료 관리          [2] 시료 주문");
        System.out.println("[3] 주문 승인/거절     [4] 모니터링");
        System.out.println("[5] 생산라인 조회      [6] 출고 처리");
        System.out.println("[0] 종료");
        System.out.println(LINE);
        System.out.print("선택 > ");
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printGoodbye() {
        System.out.println("프로그램을 종료합니다.");
    }
}
