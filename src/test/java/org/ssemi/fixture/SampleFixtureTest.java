package org.ssemi.fixture;

import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.Sample;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleFixtureTest {

    @Test
    void generate_개수_일치() {
        List<Sample> samples = SampleFixture.generate(5, 42L);
        assertEquals(5, samples.size());
    }

    @Test
    void generate_ID_포맷_S_NNN() {
        List<Sample> samples = SampleFixture.generate(5, 42L);
        assertEquals("S-001", samples.get(0).getSampleId());
        assertEquals("S-005", samples.get(4).getSampleId());
    }

    @Test
    void generate_동일_seed_재현성() {
        List<Sample> first  = SampleFixture.generate(3, 42L);
        List<Sample> second = SampleFixture.generate(3, 42L);
        assertEquals(first.get(0).getName(), second.get(0).getName());
        assertEquals(first.get(1).getName(), second.get(1).getName());
        assertEquals(first.get(2).getName(), second.get(2).getName());
    }

    @Test
    void generate_yield_범위() {
        List<Sample> samples = SampleFixture.generate(100, 99L);
        for (Sample s : samples) {
            assertTrue(s.getYield() >= 0.60, "yield 최솟값 0.60 위반: " + s.getYield());
            assertTrue(s.getYield() <= 0.99, "yield 최댓값 0.99 위반: " + s.getYield());
        }
    }

    @Test
    void generate_count_0이하_빈리스트() {
        assertTrue(SampleFixture.generate(0, 42L).isEmpty());
        assertTrue(SampleFixture.generate(-1, 42L).isEmpty());
    }

    @Test
    void generate_count_1000이상_예외() {
        assertThrows(IllegalArgumentException.class, () -> SampleFixture.generate(1000, 42L));
    }
}
