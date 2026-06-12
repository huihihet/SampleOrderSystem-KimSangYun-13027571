package org.ssemi.controller;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.view.ReleaseView;

import java.util.List;
import java.util.Scanner;

public class ReleaseController {

    private final OrderRepository orderRepo;
    private final ReleaseView view;
    private final Scanner scanner;

    public ReleaseController(OrderRepository orderRepo,
                             ReleaseView view,
                             Scanner scanner) {
        this.orderRepo = orderRepo;
        this.view      = view;
        this.scanner   = scanner;
    }

    public void processRelease() {
        List<Order> confirmedOrders = orderRepo.findByStatus(OrderStatus.CONFIRMED);
        if (confirmedOrders.isEmpty()) {
            view.printEmpty();
            return;
        }

        view.printOrderList(confirmedOrders);
        view.printPrompt("출고할 주문 번호 > ");

        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index < 0 || index >= confirmedOrders.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }

        Order order = confirmedOrders.get(index);
        order.setStatus(OrderStatus.RELEASE);
        orderRepo.update(order);
        view.printSuccess("출고 완료: " + order.getOrderId());
    }
}
