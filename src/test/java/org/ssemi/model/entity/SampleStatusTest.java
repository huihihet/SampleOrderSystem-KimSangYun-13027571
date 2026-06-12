package org.ssemi.model.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SampleStatusTest {

    @Test
    void record_생성_및_필드_접근() {
        SampleStatus status = new SampleStatus("S-001", "실리콘 웨이퍼", 480, "여유");

        assertEquals("S-001", status.sampleId());
        assertEquals("실리콘 웨이퍼", status.name());
        assertEquals(480, status.stock());
        assertEquals("여유", status.stockLevel());
    }

    @Test
    void sampleId_타입_String_확인() {
        SampleStatus status = new SampleStatus("S-001", "웨이퍼", 100, "여유");

        assertInstanceOf(String.class, status.sampleId());
    }

    @Test
    void equals_동일_값_true() {
        SampleStatus s1 = new SampleStatus("S-001", "웨이퍼", 100, "여유");
        SampleStatus s2 = new SampleStatus("S-001", "웨이퍼", 100, "여유");

        assertEquals(s1, s2);
    }
}
