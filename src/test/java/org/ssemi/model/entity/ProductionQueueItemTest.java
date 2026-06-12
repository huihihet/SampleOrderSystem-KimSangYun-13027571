package org.ssemi.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductionQueueItemTest {

    @Test
    void 전체_인자_생성자_후_getter_검증() {
        ProductionQueueItem item = new ProductionQueueItem(
            "Q-001", "ORD-20260612-0001", "S-001",
            100, 50, 12, 360, "2026-06-12T10:00:00"
        );

        assertEquals("Q-001", item.getQueueId());
        assertEquals("ORD-20260612-0001", item.getOrderId());
        assertEquals("S-001", item.getSampleId());
        assertEquals(100, item.getOrderQuantity());
        assertEquals(50, item.getRequiredQuantity());
        assertEquals(12, item.getActualProductionQuantity());
        assertEquals(360, item.getTotalProductionTime());
        assertEquals("2026-06-12T10:00:00", item.getEnqueuedAt());
    }

    @Test
    void noarg_생성자_setter_검증() {
        ProductionQueueItem item = new ProductionQueueItem();
        item.setQueueId("Q-002");
        item.setOrderId("ORD-20260612-0002");
        item.setSampleId("S-002");
        item.setOrderQuantity(80);
        item.setRequiredQuantity(30);
        item.setActualProductionQuantity(8);
        item.setTotalProductionTime(240);
        item.setEnqueuedAt("2026-06-12T11:00:00");

        assertEquals("Q-002", item.getQueueId());
        assertEquals("ORD-20260612-0002", item.getOrderId());
        assertEquals("S-002", item.getSampleId());
        assertEquals(80, item.getOrderQuantity());
        assertEquals(30, item.getRequiredQuantity());
        assertEquals(8, item.getActualProductionQuantity());
        assertEquals(240, item.getTotalProductionTime());
        assertEquals("2026-06-12T11:00:00", item.getEnqueuedAt());
    }
}
