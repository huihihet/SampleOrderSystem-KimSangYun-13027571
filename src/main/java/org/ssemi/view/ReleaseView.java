package org.ssemi.view;

import org.ssemi.model.entity.Order;

import java.util.List;

public class ReleaseView {

    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printOrderList(List<Order> orders) {
        clearScreen();
        System.out.println("=".repeat(76));
        System.out.println("  출고 처리");
        System.out.println("=".repeat(76));
        System.out.println(
            ViewUtil.padRight("번호", 6) +
            ViewUtil.padRight("주문번호", 22) +
            ViewUtil.padRight("시료ID", 10) +
            ViewUtil.padRight("고객명", 22) +
            ViewUtil.padRight("수량", 9) +
            "상태");
        System.out.println("-".repeat(76));
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.println(
                ViewUtil.padRight(String.valueOf(i + 1), 6) +
                ViewUtil.padRight(o.getOrderId(), 22) +
                ViewUtil.padRight(o.getSampleId(), 10) +
                ViewUtil.padRight(o.getCustomerName(), 22) +
                ViewUtil.padRight(o.getQuantity() + " ea", 9) +
                o.getStatus());
        }
        System.out.println("-".repeat(76));
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
