package org.ssemi.model.entity;

import java.util.Objects;

public class Sample {

    private String sampleId;
    private String name;
    private int avgProductionTime;
    private double yield;
    private int stock;

    public Sample() {}

    public Sample(String sampleId, String name, int avgProductionTime, double yield, int stock) {
        this.sampleId = sampleId;
        this.name = name;
        this.avgProductionTime = avgProductionTime;
        this.yield = yield;
        this.stock = stock;
    }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAvgProductionTime() { return avgProductionTime; }
    public void setAvgProductionTime(int avgProductionTime) { this.avgProductionTime = avgProductionTime; }

    public double getYield() { return yield; }
    public void setYield(double yield) { this.yield = yield; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    // Controller에서 stock 직접 조작 금지 — 불변식(stock >= 0) 보장
    public void deductStock(int quantity) {
        if (this.stock - quantity < 0) {
            throw new IllegalStateException(
                "재고 부족: 현재 재고 " + this.stock + " ea, 차감 요청 " + quantity + " ea"
            );
        }
        this.stock -= quantity;
    }

    // 생산 완료 시 호출
    public void addStock(int quantity) {
        this.stock += quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sample)) return false;
        Sample sample = (Sample) o;
        return Objects.equals(sampleId, sample.sampleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleId);
    }
}
