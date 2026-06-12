package org.ssemi.controller;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.OrderView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class OrderController {

    private final SampleRepository sampleRepo;
    private final OrderRepository orderRepo;
    private final ProductionLineController productionLineController;
    private final OrderView view;
    private final Scanner scanner;

    public OrderController(SampleRepository sampleRepo,
                           OrderRepository orderRepo,
                           ProductionLineController productionLineController,
                           OrderView view,
                           Scanner scanner) {
        this.sampleRepo               = sampleRepo;
        this.orderRepo                = orderRepo;
        this.productionLineController = productionLineController;
        this.view                     = view;
        this.scanner                  = scanner;
    }

    public void placeOrder() {
        List<Sample> samples = sampleRepo.findAll();
        if (samples.isEmpty()) {
            view.printError("등록된 시료가 없습니다.");
            return;
        }

        view.printSampleCatalog(samples);
        view.printPrompt("번호 선택 (0=취소) > ");
        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index == -1) return;
        if (index < 0 || index >= samples.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        Sample selected = samples.get(index);
        String sampleId = selected.getSampleId();

        view.printPrompt("고객명 > ");
        String customerName = scanner.nextLine();
        if (customerName.isBlank()) {
            view.printError("고객명을 입력해 주세요.");
            return;
        }

        view.printPrompt("주문 수량 > ");
        int quantity;
        try {
            quantity = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            view.printError("올바른 숫자를 입력해 주세요.");
            return;
        }
        if (quantity <= 0) {
            view.printError("주문 수량은 1 이상이어야 합니다.");
            return;
        }

        view.printOrderConfirm(selected, customerName, quantity);
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            view.printError("주문이 취소되었습니다.");
            return;
        }

        String orderId = "ORD-"
            + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
            + "-" + String.format("%04d", orderRepo.findAll().size() + 1);

        orderRepo.save(new Order(orderId, sampleId, customerName, quantity, OrderStatus.RESERVED));
        view.printSuccess("주문이 접수되었습니다: " + orderId);
    }

    public void approveOrder() {
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        if (reserved.isEmpty()) {
            view.printEmpty();
            return;
        }

        Map<String, String> sampleNames = buildSampleNames(reserved);
        view.printOrderList(reserved, sampleNames);
        view.printPrompt("승인할 주문 번호 > ");

        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index < 0 || index >= reserved.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }

        Order order = reserved.get(index);
        Optional<Sample> sampleOpt = sampleRepo.findById(order.getSampleId());
        if (sampleOpt.isEmpty()) {
            view.printError("시료를 찾을 수 없습니다: " + order.getSampleId());
            return;
        }
        Sample sample = sampleOpt.get();

        if (sample.getStock() >= order.getQuantity()) {
            // 재고 차감은 출고(ReleaseController) 시점에 수행
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepo.update(order);
            view.printApprovalDetail(sample, order, 0, 0, 0);
        } else {
            // 표시용 수치를 update 전에 미리 계산
            int requiredQty   = order.getQuantity() - sample.getStock();
            int actualProdQty = (int) Math.ceil(requiredQty / (sample.getYield() * 0.9));
            int totalProdTime = sample.getAvgProductionTime(); // 단위당 초수

            order.setStatus(OrderStatus.PRODUCING);
            orderRepo.update(order);
            productionLineController.registerProductionQueue(order, sample);

            view.printApprovalDetail(sample, order, requiredQty, actualProdQty, totalProdTime);
        }
    }

    public void rejectOrder() {
        List<Order> reserved = orderRepo.findByStatus(OrderStatus.RESERVED);
        if (reserved.isEmpty()) {
            view.printEmpty();
            return;
        }

        Map<String, String> sampleNames = buildSampleNames(reserved);
        view.printOrderList(reserved, sampleNames);
        view.printPrompt("거절할 주문 번호 > ");

        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index < 0 || index >= reserved.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }

        Order order = reserved.get(index);
        order.setStatus(OrderStatus.REJECTED);
        orderRepo.update(order);
        view.printSuccess("주문이 거절되었습니다: " + order.getOrderId());
    }

    public void handleSubMenu() {
        while (true) {
            view.printApprovalMenu();
            int choice = readMenuChoice();
            switch (choice) {
                case 1 -> { approveOrder(); pause(); }
                case 2 -> { rejectOrder();  pause(); }
                case 0 -> { return; }
                default -> view.printError("올바른 번호를 입력해 주세요.");
            }
        }
    }

    private void pause() {
        view.printPause();
        scanner.nextLine();
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private Map<String, String> buildSampleNames(List<Order> orders) {
        Map<String, String> names = new HashMap<>();
        for (Order o : orders) {
            if (!names.containsKey(o.getSampleId())) {
                sampleRepo.findById(o.getSampleId())
                    .ifPresentOrElse(
                        s -> names.put(s.getSampleId(), s.getName()),
                        () -> names.put(o.getSampleId(), o.getSampleId())
                    );
            }
        }
        return names;
    }
}
