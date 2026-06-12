package org.ssemi.view;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;

import java.util.List;
import java.util.Map;

public class OrderView {

    private static final String RESET  = "\033[0m";
    private static final String BLUE   = "\033[34m";
    private static final String GREEN  = "\033[32m";
    private static final String ORANGE = "\033[33m";
    private static final String RED    = "\033[31m";
    private static final String PURPLE = "\033[35m";
    private static final String LINE   = "-".repeat(72);

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printApprovalMenu() {
        clearScreen();
        System.out.println("=".repeat(63));
        System.out.println("  주문 승인 / 거절");
        System.out.println("=".repeat(63));
        System.out.println("[1] 주문 승인");
        System.out.println("[2] 주문 거절");
        System.out.println("[0] 뒤로");
        System.out.print("선택 > ");
    }

    public void printSampleCatalog(List<Sample> samples) {
        clearScreen();
        System.out.println("=".repeat(63));
        System.out.println("  시료 주문");
        System.out.println("=".repeat(63));
        System.out.printf("  %-4s %-10s %-20s %-10s %-6s %s%n",
            "번호", "ID", "시료명", "생산시간", "수율", "현재 재고");
        System.out.println("  " + "-".repeat(61));
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            String stockStr = s.getStock() == 0
                ? RED + "0 ea (고갈)" + RESET
                : s.getStock() < 50
                    ? ORANGE + s.getStock() + " ea" + RESET
                    : s.getStock() + " ea";
            System.out.printf("  %-4d %-10s %-20s %-10s %-6s %s%n",
                i + 1,
                s.getSampleId(),
                s.getName(),
                s.getAvgProductionTime() + " sec/ea",
                String.format("%.0f%%", s.getYield() * 100),
                stockStr);
        }
        System.out.println("  " + "-".repeat(61));
        System.out.println();
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printOrderList(List<Order> orders, Map<String, String> sampleNames) {
        System.out.printf("%-6s %-22s %-20s %-12s %-8s %s%n",
            "번호", "주문번호", "시료명", "고객명", "수량", "상태");
        System.out.println(LINE);
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = sampleNames.getOrDefault(o.getSampleId(), o.getSampleId());
            System.out.printf("%-6d %-22s %-20s %-12s %-8s %s%n",
                i + 1,
                o.getOrderId(),
                sampleName,
                o.getCustomerName(),
                o.getQuantity() + " ea",
                colorStatus(o.getStatus()));
        }
        System.out.println(LINE);
    }

    public void printOrderConfirm(Sample sample, String customerName, int quantity) {
        System.out.println();
        System.out.println("[주문 확인]");
        System.out.printf("시료: %s (%s)%n", sample.getName(), sample.getSampleId());
        System.out.printf("고객: %s%n", customerName);
        System.out.printf("수량: %d ea%n", quantity);
        System.out.println();
        System.out.print("진행하시겠습니까? (Y/N) > ");
    }

    public void printApprovalDetail(Sample sample, Order order,
                                    int requiredQty, int actualProdQty, int prodTime) {
        System.out.println();
        if (requiredQty == 0) {
            System.out.printf("시료 %s  현재 재고 %d ea  주문 수량 %d ea  부족분 0 ea%n",
                sample.getName(), sample.getStock(), order.getQuantity());
            System.out.println("상태 변경  RESERVED → " + GREEN + "CONFIRMED" + RESET);
        } else {
            System.out.printf("시료 %s  현재 재고 %d ea  주문 수량 %d ea  부족분 %d ea%n",
                sample.getName(), sample.getStock(), order.getQuantity(), requiredQty);
            System.out.printf("실생산량 %d ea  (단위당 %d초 × %d ea = %d초)%n",
                actualProdQty, prodTime, actualProdQty, (long) prodTime * actualProdQty);
            System.out.println("상태 변경  RESERVED → " + ORANGE + "PRODUCING" + RESET);
        }
    }

    public void printSuccess(String message) {
        System.out.println("[성공] " + message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printEmpty() {
        System.out.println("처리할 주문이 없습니다.");
    }

    public void printPause() {
        System.out.println();
        System.out.print("  [ Enter 키로 계속 ]  ");
    }

    private String colorStatus(OrderStatus status) {
        String color = switch (status) {
            case RESERVED  -> BLUE;
            case PRODUCING -> ORANGE;
            case CONFIRMED -> GREEN;
            case RELEASE   -> PURPLE;
            case REJECTED  -> RED;
        };
        return color + status.name() + RESET;
    }
}
