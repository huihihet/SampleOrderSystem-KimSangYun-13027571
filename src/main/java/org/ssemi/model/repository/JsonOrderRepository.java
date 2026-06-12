package org.ssemi.model.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonOrderRepository implements OrderRepository {

    private static final Type LIST_TYPE = new TypeToken<List<Order>>() {}.getType();

    private final Path filePath;

    public JsonOrderRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Order order) {
        List<Order> all = findAll();
        boolean duplicated = all.stream()
            .anyMatch(o -> o.getOrderId().equals(order.getOrderId()));
        if (duplicated) {
            throw new IllegalArgumentException("이미 존재하는 주문 ID: " + order.getOrderId());
        }
        all.add(order);
        JsonFileUtil.writeList(filePath, all);
    }

    @Override
    public Optional<Order> findById(String orderId) {
        return findAll().stream()
            .filter(o -> o.getOrderId().equals(orderId))
            .findFirst();
    }

    @Override
    public List<Order> findAll() {
        return JsonFileUtil.readList(filePath, LIST_TYPE);
    }

    @Override
    public List<Order> findByStatus(OrderStatus status) {
        return findAll().stream()
            .filter(o -> o.getStatus() == status)
            .collect(Collectors.toList());
    }

    // null 검증 없음 — 호출자 책임 (내부 API)
    @Override
    public List<Order> findBySampleId(String sampleId) {
        return findAll().stream()
            .filter(o -> sampleId.equals(o.getSampleId()))
            .collect(Collectors.toList());
    }

    @Override
    public void update(Order order) {
        List<Order> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getOrderId().equals(order.getOrderId())) {
                all.set(i, order);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new NoSuchElementException("존재하지 않는 주문 ID: " + order.getOrderId());
        }
        JsonFileUtil.writeList(filePath, all);
    }
}
