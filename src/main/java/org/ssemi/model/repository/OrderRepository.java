package org.ssemi.model.repository;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findById(String orderId);
    List<Order> findAll();
    List<Order> findByStatus(OrderStatus status);
    List<Order> findBySampleId(String sampleId);
    void update(Order order);
}
