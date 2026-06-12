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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

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
        renderQueue(); // 첫 렌더는 호출 스레드에서 즉시 실행
        AtomicBoolean active = new AtomicBoolean(true);
        Thread t = new Thread(() -> {
            while (active.get()) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                if (active.get()) renderQueue();
            }
        }, "prodline-refresh");
        t.setDaemon(true);
        t.start();
        try { scanner.nextLine(); } catch (NoSuchElementException ignored) {}
        active.set(false);
        t.interrupt();
        try { t.join(300); } catch (InterruptedException ignored) {}
    }

    private void renderQueue() {
        checkAndAutoComplete();
        List<ProductionQueueItem> items = queueRepo.findAll();
        if (items.isEmpty()) {
            view.printEmpty();
            view.printExitHint();
            return;
        }
        Map<String, String> sampleNames = buildSampleNames(items);
        view.printQueueList(items, sampleNames);
        view.printExitHint();
    }

    // FIFO: 단위가 완료될 때마다 재고 증가. 전체 완료 시 주문 CONFIRMED + 큐 제거.
    public void checkAndAutoComplete() {
        ensureFirstItemActive();
        LocalDateTime now = LocalDateTime.now();
        for (ProductionQueueItem item : List.copyOf(queueRepo.findAll())) {
            int secsEach = item.getTotalProductionTime();
            int totalQty = item.getActualProductionQuantity();
            if (secsEach <= 0 || totalQty <= 0) continue;

            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(item.getEnqueuedAt());
            } catch (Exception e) {
                continue;
            }
            if (!now.isAfter(startTime)) continue; // 아직 시작 안 됨

            long elapsed = ChronoUnit.SECONDS.between(startTime, now);
            int shouldHaveProduced = (int) Math.min(elapsed / secsEach, totalQty);
            int alreadyProduced = item.getProducedQuantity();
            int newlyProduced = shouldHaveProduced - alreadyProduced;
            if (newlyProduced <= 0) continue;

            final int toAdd = newlyProduced;
            sampleRepo.findById(item.getSampleId()).ifPresent(sample -> {
                sample.addStock(toAdd);
                sampleRepo.update(sample);
            });

            if (shouldHaveProduced >= totalQty) {
                orderRepo.findById(item.getOrderId()).ifPresent(order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepo.update(order);
                });
                queueRepo.deleteById(item.getQueueId());
            } else {
                item.setProducedQuantity(shouldHaveProduced);
                queueRepo.update(item);
            }
        }
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
        int remaining = item.getActualProductionQuantity() - item.getProducedQuantity();
        sample.addStock(remaining);
        sampleRepo.update(sample);

        orderRepo.findById(item.getOrderId()).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepo.update(order);
        });

        queueRepo.deleteById(item.getQueueId());

        view.printSuccess("생산 완료: " + sample.getName()
            + " " + item.getActualProductionQuantity() + " ea → 재고 반영, 주문 CONFIRMED");
    }

    public void handleSubMenu() {
        while (true) {
            view.printMenu();
            int choice = readMenuChoice();
            switch (choice) {
                case 1 -> showQueue();
                case 2 -> { completeProduction(); pause(); }
                case 0 -> { return; }
                default -> view.printError("올바른 번호를 입력해 주세요.");
            }
        }
    }

    private void pause() {
        view.printPause();
        try { scanner.nextLine(); } catch (NoSuchElementException ignored) {}
    }

    public int getQueueWaitingCount() {
        return queueRepo.findAll().size();
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
        int totalProdTime = sample.getAvgProductionTime(); // 단위당 생산 시간(초)

        // FIFO: 마지막 항목 완료 시각이 새 항목의 시작 시각
        LocalDateTime startTime = LocalDateTime.now();
        for (ProductionQueueItem existing : queueRepo.findAll()) {
            try {
                LocalDateTime existingEnd = LocalDateTime.parse(existing.getEnqueuedAt())
                    .plusSeconds((long) existing.getTotalProductionTime()
                                 * existing.getActualProductionQuantity());
                if (existingEnd.isAfter(startTime)) startTime = existingEnd;
            } catch (Exception ignored) {}
        }

        String queueId = "Q-" + String.format("%03d", queueRepo.findAll().size() + 1);
        String enqueuedAt = startTime.toString();
        queueRepo.enqueue(new ProductionQueueItem(
            queueId, order.getOrderId(), order.getSampleId(),
            order.getQuantity(), requiredQty, actualProdQty, totalProdTime, enqueuedAt));
    }

    // 첫 번째 항목이 미래 시각이면 전체 큐를 now부터 재스케줄링 (FIFO 즉시 시작 보장)
    private void ensureFirstItemActive() {
        List<ProductionQueueItem> items = queueRepo.findAll();
        if (items.isEmpty()) return;
        try {
            LocalDateTime firstStart = LocalDateTime.parse(items.get(0).getEnqueuedAt());
            if (!firstStart.isAfter(LocalDateTime.now())) return;
            LocalDateTime cursor = LocalDateTime.now();
            for (ProductionQueueItem item : items) {
                item.setEnqueuedAt(cursor.toString());
                queueRepo.update(item);
                cursor = cursor.plusSeconds(
                    (long) item.getTotalProductionTime() * item.getActualProductionQuantity());
            }
        } catch (Exception ignored) {}
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
