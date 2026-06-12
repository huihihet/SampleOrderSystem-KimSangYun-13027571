package org.ssemi.controller;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.ProductionQueueItem;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.ProductionQueueRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.ProductionLineView;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class ProductionLineController {

    private final SampleRepository sampleRepo;
    private final OrderRepository orderRepo;
    private final ProductionQueueRepository queueRepo;
    private final ProductionLineView view;
    private final Scanner scanner;

    public ProductionLineController(SampleRepository sampleRepo,
                                    OrderRepository orderRepo,
                                    ProductionQueueRepository queueRepo,
                                    ProductionLineView view,
                                    Scanner scanner) {
        this.sampleRepo = sampleRepo;
        this.orderRepo  = orderRepo;
        this.queueRepo  = queueRepo;
        this.view       = view;
        this.scanner    = scanner;
    }

    public void showQueue() {
        List<ProductionQueueItem> items = queueRepo.findAll();
        if (items.isEmpty()) {
            view.printEmpty();
            return;
        }
        Map<String, String> sampleNames = buildSampleNames(items);
        view.printQueueList(items, sampleNames);
    }

    public void completeProduction() {
        List<ProductionQueueItem> items = queueRepo.findAll();
        if (items.isEmpty()) {
            view.printEmpty();
            return;
        }
        Map<String, String> sampleNames = buildSampleNames(items);
        view.printQueueList(items, sampleNames);
        view.printPrompt("완료할 항목 번호 > ");

        int index;
        try {
            index = Integer.parseInt(scanner.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }
        if (index < 0 || index >= items.size()) {
            view.printError("올바른 번호를 입력해 주세요.");
            return;
        }

        ProductionQueueItem item = items.get(index);

        Optional<Sample> sampleOpt = sampleRepo.findById(item.getSampleId());
        if (sampleOpt.isEmpty()) {
            view.printError("시료를 찾을 수 없습니다: " + item.getSampleId());
            return;
        }
        Sample sample = sampleOpt.get();
        sample.addStock(item.getActualProductionQuantity());
        sampleRepo.update(sample);

        orderRepo.findById(item.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepo.update(order);
        });

        queueRepo.deleteById(item.getQueueId());

        view.printSuccess("생산 완료: " + sample.getName()
            + " " + item.getActualProductionQuantity() + " ea → 재고 반영, 주문 CONFIRMED");
    }

    // OrderController에서 재고 부족 승인 시 호출
    public void registerProductionQueue(Order order, Sample sample) {
        int requiredQty = order.getQuantity() - sample.getStock();
        double effectiveYield = sample.getYield() * 0.9;
        if (effectiveYield <= 0.0) {
            view.printError("수율이 0인 시료는 생산할 수 없습니다.");
            return;
        }
        int actualProdQty = (int) Math.ceil(requiredQty / effectiveYield);
        int totalProdTime = sample.getAvgProductionTime() * actualProdQty;
        String queueId = "Q-" + String.format("%03d", queueRepo.findAll().size() + 1);
        String enqueuedAt = LocalDateTime.now().toString();
        queueRepo.enqueue(new ProductionQueueItem(
            queueId, order.getOrderId(), order.getSampleId(),
            order.getQuantity(), requiredQty, actualProdQty, totalProdTime, enqueuedAt));
    }

    private Map<String, String> buildSampleNames(List<ProductionQueueItem> items) {
        Map<String, String> names = new HashMap<>();
        for (ProductionQueueItem item : items) {
            if (!names.containsKey(item.getSampleId())) {
                sampleRepo.findById(item.getSampleId())
                    .ifPresentOrElse(
                        s -> names.put(s.getSampleId(), s.getName()),
                        () -> names.put(item.getSampleId(), item.getSampleId())
                    );
            }
        }
        return names;
    }
}
