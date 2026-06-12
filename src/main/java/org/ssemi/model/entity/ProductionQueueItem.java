package org.ssemi.model.entity;

public class ProductionQueueItem {

    private String queueId;
    private String orderId;
    private String sampleId;
    private int orderQuantity;
    private int requiredQuantity;
    private int actualProductionQuantity;
    private int totalProductionTime;
    private String enqueuedAt;

    public ProductionQueueItem() {}

    public ProductionQueueItem(String queueId, String orderId, String sampleId,
                                int orderQuantity, int requiredQuantity,
                                int actualProductionQuantity,
                                int totalProductionTime, String enqueuedAt) {
        this.queueId = queueId;
        this.orderId = orderId;
        this.sampleId = sampleId;
        this.orderQuantity = orderQuantity;
        this.requiredQuantity = requiredQuantity;
        this.actualProductionQuantity = actualProductionQuantity;
        this.totalProductionTime = totalProductionTime;
        this.enqueuedAt = enqueuedAt;
    }

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getSampleId() { return sampleId; }
    public void setSampleId(String sampleId) { this.sampleId = sampleId; }

    public int getOrderQuantity() { return orderQuantity; }
    public void setOrderQuantity(int orderQuantity) { this.orderQuantity = orderQuantity; }

    public int getRequiredQuantity() { return requiredQuantity; }
    public void setRequiredQuantity(int requiredQuantity) { this.requiredQuantity = requiredQuantity; }

    public int getActualProductionQuantity() { return actualProductionQuantity; }
    public void setActualProductionQuantity(int actualProductionQuantity) { this.actualProductionQuantity = actualProductionQuantity; }

    public int getTotalProductionTime() { return totalProductionTime; }
    public void setTotalProductionTime(int totalProductionTime) { this.totalProductionTime = totalProductionTime; }

    public String getEnqueuedAt() { return enqueuedAt; }
    public void setEnqueuedAt(String enqueuedAt) { this.enqueuedAt = enqueuedAt; }
}
