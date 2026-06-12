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
        System.out.println("=".repeat(70));
        System.out.println("  시료 주문");
        System.out.println("=".repeat(70));
        System.out.println("  " +
            ViewUtil.padRight("번호", 6) +
            ViewUtil.padRight("ID", 10) +
            ViewUtil.padRight("시료명", 22) +
            ViewUtil.padRight("생산시간", 12) +
            ViewUtil.padRight("수율", 7) +
            "현재 재고");
        System.out.println("  " + "-".repeat(62));
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            String stockStr = s.getStock() == 0
                ? RED + "0 ea (고갈)" + RESET
                : s.getStock() < 50
                    ? ORANGE + s.getStock() + " ea" + RESET
                    : s.getStock() + " ea";
            System.out.println("  " +
                ViewUtil.padRight(String.valueOf(i + 1), 6) +
                ViewUtil.padRight(s.getSampleId(), 10) +
                ViewUtil.padRight(s.getName(), 22) +
                ViewUtil.padRight(s.getAvgProductionTime() + " sec/ea", 12) +
                ViewUtil.padRight(String.format("%.0f%%", s.getYield() * 100), 7) +
                stockStr);
        }
        System.out.println("  " + "-".repeat(62));
        System.out.println();
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printOrderList(List<Order> orders, Map<String, String> sampleNames) {
        System.out.println(
            ViewUtil.padRight("번호", 5) +
            ViewUtil.padRight("주문번호", 22) +
            ViewUtil.padRight("시료명", 22) +
            ViewUtil.padRight("고객명", 16) +
            ViewUtil.padRight("수량", 9) +
            "상태");
        System.out.println(LINE);
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = sampleNames.getOrDefault(o.getSampleId(), o.getSampleId());
            System.out.println(
                ViewUtil.padRight(String.valueOf(i + 1), 5) +
                ViewUtil.padRight(o.getOrderId(), 22) +
                ViewUtil.padRight(sampleName, 22) +
                ViewUtil.padRight(o.getCustomerName(), 16) +
                ViewUtil.padRight(o.getQuantity() + " ea", 9) +
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
