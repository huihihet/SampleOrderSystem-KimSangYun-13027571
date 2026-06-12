package org.ssemi.fixture;

import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderFixtureTest {

    @Test
    void generate_개수_일치() {
        List<String> sampleIds = List.of("S-001", "S-002");
        List<Order> orders = OrderFixture.generate(sampleIds, 3, 42L);
        assertEquals(3, orders.size());
    }

    @Test
    void generate_ID_포맷_ORD() {
        List<String> sampleIds = List.of("S-001");
        List<Order> orders = OrderFixture.generate(sampleIds, 1, 42L);
        assertEquals("ORD-20260101-0001", orders.get(0).getOrderId());
    }

    @Test
    void generate_sampleId_참조_무결성() {
        List<String> sampleIds = List.of("S-001", "S-002", "S-003");
        List<Order> orders = OrderFixture.generate(sampleIds, 20, 42L);
        for (Order o : orders) {
            assertTrue(sampleIds.contains(o.getSampleId()),
                "sampleId 참조 무결성 위반: " + o.getSampleId());
        }
    }

    @Test
    void generate_전체_RESERVED() {
        List<String> sampleIds = List.of("S-001");
        List<Order> orders = OrderFixture.generate(sampleIds, 5, 42L);
        for (Order o : orders) {
            assertEquals(OrderStatus.RESERVED, o.getStatus());
        }
    }

    @Test
    void generate_빈_sampleIds_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> OrderFixture.generate(List.of(), 3, 42L));
    }

    @Test
    void generate_null_sampleIds_IllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> OrderFixture.generate(null, 3, 42L));
    }

    @Test
    void generate_count_0이하_빈리스트() {
        List<String> sampleIds = List.of("S-001");
        assertTrue(OrderFixture.generate(sampleIds, 0, 42L).isEmpty());
        assertTrue(OrderFixture.generate(sampleIds, -1, 42L).isEmpty());
    }
}
