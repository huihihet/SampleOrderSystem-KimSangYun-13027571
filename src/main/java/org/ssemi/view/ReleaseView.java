package org.ssemi.view;

import org.ssemi.model.entity.Order;

import java.util.List;

public class ReleaseView {

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printOrderList(List<Order> orders) {
        System.out.printf("%-5s %-22s %-9s %-18s %-8s %s%n",
            "번호", "주문번호", "시료ID", "고객명", "수량", "상태");
        System.out.println("-".repeat(72));
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.printf("%-5d %-22s %-9s %-18s %-8s %s%n",
                i + 1,
                o.getOrderId(),
                o.getSampleId(),
                o.getCustomerName(),
                o.getQuantity() + "ea",
                o.getStatus());
        }
        System.out.println("-".repeat(72));
    }

    public void printSuccess(String message) {
        System.out.println("[성공] " + message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printEmpty() {
        System.out.println("출고 대기 주문이 없습니다.");
    }
}
