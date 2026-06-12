package org.ssemi.fixture;

import org.ssemi.model.entity.Order;
import org.ssemi.model.entity.OrderStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OrderFixture {

    private static final String[] CUSTOMERS = {
        "Seoul Fab", "KAIST Lab", "SNU Research", "Yonsei Fab", "POSTECH Lab",
        "Korea Chip", "Nano Systems", "Alpha Fabless", "Beta Research", "Gamma Semi"
    };

    public static List<Order> generate(List<String> sampleIds, int count, long seed) {
        if (sampleIds == null || sampleIds.isEmpty())
            throw new IllegalArgumentException("sampleIds는 비어 있을 수 없습니다.");
        if (count <= 0) return List.of();

        Random random = new Random(seed);
        List<Order> orders = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String orderId      = "ORD-20260101-" + String.format("%04d", i);
            String sampleId     = sampleIds.get(random.nextInt(sampleIds.size()));
            String customerName = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
            int    quantity     = random.nextInt(100) + 1;
            orders.add(new Order(orderId, sampleId, customerName, quantity, OrderStatus.RESERVED));
        }
        return orders;
    }
}
