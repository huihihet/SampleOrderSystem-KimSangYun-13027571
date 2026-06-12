package org.ssemi.fixture;

import org.ssemi.model.entity.Sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SampleFixture {

    private static final String[] MATERIALS =
        {"GaN", "SiC", "Si", "GaAs", "InP", "Ge", "SiGe", "AlGaN"};

    public static List<Sample> generate(int count, long seed) {
        if (count <= 0) return List.of();
        if (count > 999) throw new IllegalArgumentException("count는 999 이하여야 합니다.");

        Random random = new Random(seed);
        List<Sample> samples = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            String material = MATERIALS[random.nextInt(MATERIALS.length)];
            String name     = material + "-" + String.format("%03d", random.nextInt(999) + 1);
            int    apt      = random.nextInt(451) + 30;
            double yield    = (random.nextInt(40) + 60) / 100.0;
            int    stock    = random.nextInt(501);
            samples.add(new Sample("S-" + String.format("%03d", i), name, apt, yield, stock));
        }
        return samples;
    }
}
