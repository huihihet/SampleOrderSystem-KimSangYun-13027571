package org.ssemi.view;

import org.ssemi.model.entity.ProductionQueueItem;

import java.util.List;
import java.util.Map;

public class ProductionLineView {

    public void printMenu() {
        System.out.println("[1] 현황 조회");
        System.out.println("[2] 생산 완료");
        System.out.println("[0] 뒤로");
        System.out.print("선택 > ");
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printQueueList(List<ProductionQueueItem> items, Map<String, String> sampleNames) {
        System.out.printf("%-6s %-22s %-20s %-9s %-9s %-10s %s%n",
            "순서", "주문번호", "시료명", "주문량", "부족분", "실생산량", "예상완료(min)");
        System.out.println("-".repeat(85));
        for (int i = 0; i < items.size(); i++) {
            ProductionQueueItem item = items.get(i);
            String sampleName = sampleNames.getOrDefault(item.getSampleId(), item.getSampleId());
            System.out.printf("%-6d %-22s %-20s %-9s %-9s %-10s %d%n",
                i + 1,
                item.getOrderId(),
                sampleName,
                item.getOrderQuantity() + " ea",
                item.getRequiredQuantity() + " ea",
                item.getActualProductionQuantity() + " ea",
                item.getTotalProductionTime());
        }
        System.out.println("-".repeat(85));
        System.out.println("* 부족분 = 주문량 - 재고, 실생산량 = ceil(부족분 / (수율 × 0.9))");
    }

    public void printSuccess(String message) {
        System.out.println("[성공] " + message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printEmpty() {
        System.out.println("생산 중인 항목이 없습니다.");
    }
}
