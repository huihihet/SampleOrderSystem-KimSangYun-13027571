package org.ssemi.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void 전체_인자_생성자_후_getter_검증() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);

        assertEquals("ORD-20260612-0001", order.getOrderId());
        assertEquals("S-001", order.getSampleId());
        assertEquals("홍길동", order.getCustomerName());
        assertEquals(100, order.getQuantity());
        assertEquals(OrderStatus.RESERVED, order.getStatus());
    }

    @Test
    void noarg_생성자_setter_검증() {
        Order order = new Order();
        order.setOrderId("ORD-20260612-0002");
        order.setSampleId("S-002");
        order.setCustomerName("김철수");
        order.setQuantity(50);
        order.setStatus(OrderStatus.PRODUCING);

        assertEquals("ORD-20260612-0002", order.getOrderId());
        assertEquals("S-002", order.getSampleId());
        assertEquals("김철수", order.getCustomerName());
        assertEquals(50, order.getQuantity());
        assertEquals(OrderStatus.PRODUCING, order.getStatus());
    }

    @Test
    void equals_동일_orderId_true() {
        Order o1 = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        Order o2 = new Order("ORD-20260612-0001", "S-002", "김철수", 200, OrderStatus.CONFIRMED);

        assertEquals(o1, o2);
    }

    @Test
    void setStatus_RESERVED에서_CONFIRMED_전환() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);
        order.setStatus(OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void setStatus_5개_상태_전체_설정() {
        Order order = new Order("ORD-20260612-0001", "S-001", "홍길동", 100, OrderStatus.RESERVED);

        order.setStatus(OrderStatus.PRODUCING);
        assertEquals(OrderStatus.PRODUCING, order.getStatus());

        order.setStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        order.setStatus(OrderStatus.RELEASE);
        assertEquals(OrderStatus.RELEASE, order.getStatus());

        order.setStatus(OrderStatus.REJECTED);
        assertEquals(OrderStatus.REJECTED, order.getStatus());
    }

    @Test
    void OrderStatus_values_길이_5() {
        assertEquals(5, OrderStatus.values().length);
    }

    @Test
    void OrderStatus_valueOf_PRODUCING_역직렬화() {
        OrderStatus status = OrderStatus.valueOf("PRODUCING");
        assertEquals(OrderStatus.PRODUCING, status);
    }
}
