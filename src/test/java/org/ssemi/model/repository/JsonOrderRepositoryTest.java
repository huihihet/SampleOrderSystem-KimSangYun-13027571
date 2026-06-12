package org.ssemi.model.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonOrderRepositoryTest {

    @TempDir
    Path tempDir;

    private Path filePath;
    private JsonOrderRepository repository;

    @BeforeEach
    void setUp() {
        filePath = tempDir.resolve("orders.json");
        repository = new JsonOrderRepository(filePath);
    }

    @Test
    void save_후_findById_조회_성공() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        repository.save(order);

        Optional<Order> found = repository.findById("ORD-20260612-0001");

        assertTrue(found.isPresent());
        assertEquals("홍길동", found.get().getCustomerName());
    }

    @Test
    void 영속성_save_후_새_인스턴스_재조회() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        repository.save(order);

        JsonOrderRepository newRepo = new JsonOrderRepository(filePath);
        Optional<Order> found = newRepo.findById("ORD-20260612-0001");

        assertTrue(found.isPresent());
        assertEquals(OrderStatus.RESERVED, found.get().getStatus());
    }

    @Test
    void 중복_orderId_save_IllegalArgumentException() {
        Order o1 = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        Order o2 = new Order("ORD-20260612-0001", "S-002", "김철수", 50, OrderStatus.RESERVED);
        repository.save(o1);

        assertThrows(IllegalArgumentException.class, () -> repository.save(o2));
    }

    @Test
    void findByStatus_RESERVED_해당_상태_주문만_반환() {
        repository.save(new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        repository.save(new Order("ORD-20260612-0002", "S-001", "김철수", 50, OrderStatus.CONFIRMED));
        repository.save(new Order("ORD-20260612-0003", "S-002", "이영희", 30, OrderStatus.RESERVED));

        List<Order> result = repository.findByStatus(OrderStatus.RESERVED);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getStatus() == OrderStatus.RESERVED));
    }

    @Test
    void findByStatus_CONFIRMED_다른_상태_제외() {
        repository.save(new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        repository.save(new Order("ORD-20260612-0002", "S-001", "김철수", 50, OrderStatus.CONFIRMED));

        List<Order> result = repository.findByStatus(OrderStatus.CONFIRMED);

        assertEquals(1, result.size());
        assertEquals("ORD-20260612-0002", result.get(0).getOrderId());
    }

    @Test
    void findBySampleId_해당_시료_주문만_반환() {
        repository.save(new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED));
        repository.save(new Order("ORD-20260612-0002", "S-002", "김철수", 50, OrderStatus.RESERVED));
        repository.save(new Order("ORD-20260612-0003", "S-001", "이영희", 30, OrderStatus.CONFIRMED));

        List<Order> result = repository.findBySampleId("S-001");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getSampleId().equals("S-001")));
    }

    @Test
    void findBySampleId_존재하지_않는_시료_빈_리스트() {
        repository.save(new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED));

        List<Order> result = repository.findBySampleId("S-999");

        assertTrue(result.isEmpty());
    }

    @Test
    void update_상태_변경_후_재조회() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        repository.save(order);

        order.setStatus(OrderStatus.CONFIRMED);
        repository.update(order);

        Optional<Order> found = repository.findById("ORD-20260612-0001");
        assertTrue(found.isPresent());
        assertEquals(OrderStatus.CONFIRMED, found.get().getStatus());
    }

    @Test
    void update_존재하지_않는_orderId_NoSuchElementException() {
        Order order = new Order("ORD-99999999-9999", "S-001", "없는주문", 100, OrderStatus.RESERVED);

        assertThrows(java.util.NoSuchElementException.class, () -> repository.update(order));
    }
}
