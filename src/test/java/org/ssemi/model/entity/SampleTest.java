package org.ssemi.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest {

    @Test
    void 전체_인자_생성자로_생성_후_getter_검증() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);

        assertEquals("S-001", sample.getSampleId());
        assertEquals("실리콘 웨이퍼", sample.getName());
        assertEquals(30, sample.getAvgProductionTime());
        assertEquals(0.92, sample.getYield());
        assertEquals(480, sample.getStock());
    }

    @Test
    void noarg_생성자_setter_후_getter_검증() {
        Sample sample = new Sample();
        sample.setSampleId("S-002");
        sample.setName("GaAs 기판");
        sample.setAvgProductionTime(45);
        sample.setYield(0.85);
        sample.setStock(200);

        assertEquals("S-002", sample.getSampleId());
        assertEquals("GaAs 기판", sample.getName());
        assertEquals(45, sample.getAvgProductionTime());
        assertEquals(0.85, sample.getYield());
        assertEquals(200, sample.getStock());
    }

    @Test
    void equals_동일_sampleId_true() {
        Sample s1 = new Sample("S-001", "웨이퍼A", 30, 0.9, 100);
        Sample s2 = new Sample("S-001", "웨이퍼B", 60, 0.8, 200);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void equals_다른_sampleId_false() {
        Sample s1 = new Sample("S-001", "웨이퍼A", 30, 0.9, 100);
        Sample s2 = new Sample("S-002", "웨이퍼A", 30, 0.9, 100);

        assertNotEquals(s1, s2);
    }

    @Test
    void deductStock_정상_차감() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 300);
        sample.deductStock(100);

        assertEquals(200, sample.getStock());
    }

    @Test
    void deductStock_재고와_동일수량_0이됨() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 100);
        sample.deductStock(100);

        assertEquals(0, sample.getStock());
    }

    @Test
    void deductStock_재고_부족시_IllegalStateException() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 50);

        assertThrows(IllegalStateException.class, () -> sample.deductStock(100));
    }

    @Test
    void deductStock_0차감_재고_변화없음() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 150);
        sample.deductStock(0);

        assertEquals(150, sample.getStock());
    }

    @Test
    void addStock_정상_증가() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 200);
        sample.addStock(50);

        assertEquals(250, sample.getStock());
    }

    @Test
    void addStock_0증가_재고_변화없음() {
        Sample sample = new Sample("S-001", "웨이퍼", 30, 0.9, 200);
        sample.addStock(0);

        assertEquals(200, sample.getStock());
    }
}
