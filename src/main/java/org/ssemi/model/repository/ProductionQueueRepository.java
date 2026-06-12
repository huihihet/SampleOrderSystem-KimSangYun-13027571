package org.ssemi.model.repository;

import org.ssemi.model.entity.ProductionQueueItem;

import java.util.List;
import java.util.Optional;

public interface ProductionQueueRepository {
    void enqueue(ProductionQueueItem item);
    List<ProductionQueueItem> findAll();
    Optional<ProductionQueueItem> findById(String queueId);
    void deleteById(String queueId);
}
