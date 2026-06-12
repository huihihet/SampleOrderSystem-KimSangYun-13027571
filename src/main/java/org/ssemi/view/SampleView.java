package org.ssemi.view;

import org.ssemi.model.entity.Sample;

import java.util.List;

public class SampleView {

    public void printMenu() {
        System.out.println("[1] 시료 등록");
        System.out.println("[2] 목록 조회");
        System.out.println("[3] 이름 검색");
        System.out.println("[0] 뒤로");
        System.out.print("선택 > ");
    }

    public void printPrompt(String prompt) {
        System.out.print(prompt);
    }

    public void printSampleList(List<Sample> samples) {
        System.out.printf("%-10s %-26s %-14s %-8s %s%n",
            "ID", "시료명", "평균생산시간", "수율", "현재재고");
        System.out.println("-".repeat(64));
        for (Sample s : samples) {
            System.out.printf("%-10s %-26s %-14s %-8s %s%n",
                s.getSampleId(),
                s.getName(),
                s.getAvgProductionTime() + " min/ea",
                String.format("%.2f", s.getYield()),
                s.getStock() + " ea");
        }
    }

    public void printSuccess(String message) {
        System.out.println("[성공] " + message);
    }

    public void printError(String message) {
        System.out.println("[오류] " + message);
    }

    public void printEmpty() {
        System.out.println("등록된 시료가 없습니다.");
    }
}
