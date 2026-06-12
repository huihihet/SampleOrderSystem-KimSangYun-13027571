package org.ssemi.model.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.ProductionQueueItem;

import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonProductionQueueRepositoryTest {

    @TempDir
    Path tempDir;

    private Path filePath;
    private JsonProductionQueueRepository repository;

    @BeforeEach
    void setUp() {
        filePath = tempDir.resolve("production_queue.json");
        repository = new JsonProductionQueueRepository(filePath);
    }

    private ProductionQueueItem createItem(String queueId, String orderId, String sampleId) {
        return new ProductionQueueItem(queueId, orderId, sampleId, 100, 50, 12, 360, "2026-06-12T10:00:00");
    }

    @Test
    void enqueue_후_findAll_목록에_포함() {
        ProductionQueueItem item = createItem("Q-001", "ORD-20260612-0001", "S-001");
        repository.enqueue(item);

        List<ProductionQueueItem> all = repository.findAll();

        assertEquals(1, all.size());
        assertEquals("Q-001", all.get(0).getQueueId());
    }

    @Test
    void 영속성_enqueue_후_새_인스턴스_재조회() {
        ProductionQueueItem item = createItem("Q-001", "ORD-20260612-0001", "S-001");
        repository.enqueue(item);

        JsonProductionQueueRepository newRepo = new JsonProductionQueueRepository(filePath);
        List<ProductionQueueItem> all = newRepo.findAll();

        assertEquals(1, all.size());
        assertEquals("Q-001", all.get(0).getQueueId());
    }

    @Test
    void 중복_queueId_enqueue_IllegalArgumentException() {
        ProductionQueueItem item1 = createItem("Q-001", "ORD-20260612-0001", "S-001");
        ProductionQueueItem item2 = createItem("Q-001", "ORD-20260612-0002", "S-002");
        repository.enqueue(item1);

        assertThrows(IllegalArgumentException.class, () -> repository.enqueue(item2));
    }

    @Test
    void findById_조회_성공() {
        ProductionQueueItem item = createItem("Q-001", "ORD-20260612-0001", "S-001");
        repository.enqueue(item);

        Optional<ProductionQueueItem> found = repository.findById("Q-001");

        assertTrue(found.isPresent());
        assertEquals("ORD-20260612-0001", found.get().getOrderId());
    }

    @Test
    void findById_존재하지_않는_ID_Optional_empty() {
        Optional<ProductionQueueItem> found = repository.findById("Q-999");

        assertTrue(found.isEmpty());
    }

    @Test
    void deleteById_삭제_후_findAll에_미포함() {
        repository.enqueue(createItem("Q-001", "ORD-20260612-0001", "S-001"));
        repository.enqueue(createItem("Q-002", "ORD-20260612-0002", "S-002"));
        repository.deleteById("Q-001");

        List<ProductionQueueItem> all = repository.findAll();

        assertEquals(1, all.size());
        assertEquals("Q-002", all.get(0).getQueueId());
    }

    @Test
    void deleteById_존재하지_않는_ID_NoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> repository.deleteById("Q-999"));
    }

    @Test
    void 순서_보장_enqueue_순서대로_findAll_반환_FIFO() {
        repository.enqueue(createItem("Q-001", "ORD-20260612-0001", "S-001"));
        repository.enqueue(createItem("Q-002", "ORD-20260612-0002", "S-002"));
        repository.enqueue(createItem("Q-003", "ORD-20260612-0003", "S-003"));

        List<ProductionQueueItem> all = repository.findAll();

        assertEquals("Q-001", all.get(0).getQueueId());
        assertEquals("Q-002", all.get(1).getQueueId());
        assertEquals("Q-003", all.get(2).getQueueId());
    }

    @Test
    void 빈_큐_findAll_빈_리스트() {
        List<ProductionQueueItem> all = repository.findAll();

        assertTrue(all.isEmpty());
    }
}
