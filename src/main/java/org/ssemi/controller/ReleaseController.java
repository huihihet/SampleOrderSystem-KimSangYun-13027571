package org.ssemi.controller;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.ReleaseView;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class ReleaseController {

    private final SampleRepository sampleRepo;
    private final OrderRepository orderRepo;
    private final ReleaseView view;
    private final Scanner scanner;

    public ReleaseController(SampleRepository sampleRepo,
                             OrderRepository orderRepo,
                             ReleaseView view,
                             Scanner scanner) {
        this.sampleRepo = sampleRepo;
        this.orderRepo  = orderRepo;
        this.view       = view;
        this.scanner    = scanner;
    }

    public void processRelease() {
        // 재고 >= 주문 수량인 CONFIRMED 주문만 출고 가능
        List<Order> releasable = orderRepo.findByStatus(OrderStatus.CONFIRMED).stream()
            .filter(o -> sampleRepo.findById(o.getSampleId())
                .map(s -> s.getStock() >= o.getQuantity())
                .orElse(false))
            .collect(Collectors.toList());

        if (releasable.isEmpty()) {
            view.printEmpty();
            return;
        }

        view.printOrderList(releasable);
        view.printPrompt("출고할 주문 번호 > ");

        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index < 0 || index >= releasable.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }

        Order order = releasable.get(index);
        Optional<Sample> sampleOpt = sampleRepo.findById(order.getSampleId());
        if (sampleOpt.isEmpty()) {
            view.printError("시료를 찾을 수 없습니다: " + order.getSampleId());
            return;
        }
        Sample sample = sampleOpt.get();
        sample.deductStock(order.getQuantity());
        sampleRepo.update(sample);

        order.setStatus(OrderStatus.RELEASE);
        orderRepo.update(order);
        view.printSuccess("출고 완료: " + order.getOrderId());
    }
}
