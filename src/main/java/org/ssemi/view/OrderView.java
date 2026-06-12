package org.ssemi.view;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;

import java.util.List;
import java.util.Map;

public class OrderView {

    private static final String RESET  = "[0m";
    private static final String BLUE   = "[34m";
    private static final String GREEN  = "[32m";
    private static final String ORANGE = "[33m";
    private static final String RED    = "[31m";
    private static final String PURPLE = "[35m";

    public void printApprovalMenu() {
        System.out.println("[1] 주문 승인");
        System.out.println("[2] 주문 거절");
        System.out.println("[0] 뒤로");
        System.out.print("선택 > ");
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printOrderList(List<Order> orders, Map<String, String> sampleNames) {
        System.out.printf("%-6s %-22s %-20s %-12s %-8s %s%n",
            "번호", "주문번호", "시료명", "고객명", "수량", "상태");
        System.out.println("-".repeat(72));
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            String sampleName = sampleNames.getOrDefault(o.getSampleId(), o.getSampleId());
            String coloredStatus = colorStatus(o.getStatus());
            System.out.printf("%-6d %-22s %-20s %-12s %-8s %s%n",
                i + 1,
                o.getOrderId(),
                sampleName,
                o.getCustomerName(),
                o.getQuantity() + " ea",
                coloredStatus);
        }
    }

    public void printApprovalDetail(Sample sample, Order order,
                                    int requiredQty, int actualProdQty, int prodTime) {
        if (requiredQty == 0) {
            System.out.printf("시료 %s  현재 재고 %d ea  주문 수량 %d ea  부족분 0 ea%n",
                sample.getName(), sample.getStock(), order.getQuantity());
            System.out.println("상태 변경  RESERVED → CONFIRMED");
        } else {
            System.out.printf("시료 %s  현재 재고 %d ea  주문 수량 %d ea  부족분 %d ea%n",
                sample.getName(), sample.getStock(), order.getQuantity(), requiredQty);
            System.out.printf("실생산량 %d ea / %d min%n", actualProdQty, prodTime);
            System.out.println("상태 변경  RESERVED → PRODUCING");
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
