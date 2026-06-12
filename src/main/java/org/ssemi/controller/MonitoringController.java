package org.ssemi.controller;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.entity.SampleStatus;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.MonitoringView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MonitoringController {

    private final SampleRepository sampleRepo;
    private final OrderRepository orderRepo;
    private final MonitoringView view;

    public MonitoringController(SampleRepository sampleRepo,
                                OrderRepository orderRepo,
                                MonitoringView view) {
        this.sampleRepo = sampleRepo;
        this.orderRepo  = orderRepo;
        this.view       = view;
    }

    public void showMonitoring() {
        List<Order> orders = orderRepo.findAll();

        Map<OrderStatus, Long> statusCounts = orders.stream()
            .filter(o -> o.getStatus() != OrderStatus.REJECTED)
            .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));

        List<SampleStatus> sampleStatusList = buildSampleStatusList(orders);

        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        view.render(statusCounts, sampleStatusList, timestamp);
        view.printExitHint();
    }

    public int getOrderCount() {
        return orderRepo.findAll().size();
    }

    public int getSampleCount() {
        return sampleRepo.findAll().size();
    }

    public long getTotalStock() {
        return sampleRepo.findAll().stream().mapToLong(Sample::getStock).sum();
    }

    public List<SampleStatus> getSampleSummary() {
        List<Order> orders = orderRepo.findAll();
        return buildSampleStatusList(orders);
    }

    private List<SampleStatus> buildSampleStatusList(List<Order> orders) {
        return sampleRepo.findAll().stream()
            .map(sample -> {
                int demandSum = orders.stream()
                    .filter(o -> o.getSampleId().equals(sample.getSampleId()))
                    .filter(o -> o.getStatus() == OrderStatus.RESERVED
                              || o.getStatus() == OrderStatus.PRODUCING
                              || o.getStatus() == OrderStatus.CONFIRMED)
                    .mapToInt(Order::getQuantity)
                    .sum();
                return new SampleStatus(
                    sample.getSampleId(),
                    sample.getName(),
                    sample.getStock(),
                    calcStockLevel(sample.getStock(), demandSum)
                );
            })
            .collect(Collectors.toList());
    }

    private String calcStockLevel(int stock, int demandSum) {
        if (stock == 0)        return "고갈";
        if (stock < demandSum) return "부족";
        return "여유";
    }
}
