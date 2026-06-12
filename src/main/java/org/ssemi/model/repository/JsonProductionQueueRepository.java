package org.ssemi.model.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.model.entity.ProductionQueueItem;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonProductionQueueRepository implements ProductionQueueRepository {

    private static final Type LIST_TYPE = new TypeToken<List<ProductionQueueItem>>() {}.getType();

    private final Path filePath;

    public JsonProductionQueueRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void enqueue(ProductionQueueItem item) {
        List<ProductionQueueItem> all = findAll();
        boolean duplicated = all.stream()
            .anyMatch(q -> q.getQueueId().equals(item.getQueueId()));
        if (duplicated) {
            throw new IllegalArgumentException("이미 존재하는 큐 ID: " + item.getQueueId());
        }
        all.add(item);
        JsonFileUtil.writeList(filePath, all);
    }

    @Override
    public List<ProductionQueueItem> findAll() {
        return JsonFileUtil.readList(filePath, LIST_TYPE);
    }

    @Override
    public Optional<ProductionQueueItem> findById(String queueId) {
        return findAll().stream()
            .filter(q -> q.getQueueId().equals(queueId))
            .findFirst();
    }

    @Override
    public void update(ProductionQueueItem item) {
        List<ProductionQueueItem> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getQueueId().equals(item.getQueueId())) {
                all.set(i, item);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new java.util.NoSuchElementException("존재하지 않는 큐 ID: " + item.getQueueId());
        }
        JsonFileUtil.writeList(filePath, all);
    }

    @Override
    public void deleteById(String queueId) {
        List<ProductionQueueItem> all = findAll();
        boolean exists = all.stream()
            .anyMatch(q -> q.getQueueId().equals(queueId));
        if (!exists) {
            throw new NoSuchElementException("존재하지 않는 큐 ID: " + queueId);
        }
        List<ProductionQueueItem> remaining = all.stream()
            .filter(q -> !q.getQueueId().equals(queueId))
            .collect(Collectors.toList());
        JsonFileUtil.writeList(filePath, remaining);
    }
}
