package org.ssemi.controller;

import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.SampleView;

import java.util.List;
import java.util.Scanner;

public class SampleController {

    private final SampleRepository repository;
    private final SampleView view;
    private final Scanner scanner;

    public SampleController(SampleRepository repository, SampleView view, Scanner scanner) {
        this.repository = repository;
        this.view = view;
        this.scanner = scanner;
    }

    public void register() {
        view.printPrompt("시료 ID (예: S-001) > ");
        String sampleId = scanner.nextLine().trim();

        view.printPrompt("시료 이름 > ");
        String name = scanner.nextLine();
        if (name.isBlank()) {
            view.printError("시료 이름을 입력해 주세요.");
            return;
        }

        view.printPrompt("평균 생산시간 (분, 정수) > ");
        int avgProductionTime;
        try {
            avgProductionTime = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            view.printError("올바른 숫자를 입력해 주세요.");
            return;
        }
        if (avgProductionTime <= 0) {
            view.printError("생산시간은 1 이상이어야 합니다.");
            return;
        }

        view.printPrompt("수율 (0.0~1.0) > ");
        double yield;
        try {
            yield = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            view.printError("올바른 숫자를 입력해 주세요.");
            return;
        }
        if (yield < 0.0 || yield > 1.0) {
            view.printError("수율은 0.0 ~ 1.0 사이여야 합니다.");
            return;
        }

        view.printPrompt("초기 재고 수량 > ");
        int stock;
        try {
            stock = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            view.printError("올바른 숫자를 입력해 주세요.");
            return;
        }
        if (stock < 0) {
            view.printError("재고는 0 이상이어야 합니다.");
            return;
        }

        if (repository.findById(sampleId).isPresent()) {
            view.printError("이미 존재하는 시료 ID입니다: " + sampleId);
            return;
        }

        repository.save(new Sample(sampleId, name, avgProductionTime, yield, stock));
        view.printSuccess("시료가 등록되었습니다: " + name);
    }

    public void listAll() {
        List<Sample> samples = repository.findAll();
        if (samples.isEmpty()) {
            view.printEmpty();
        } else {
            view.printSampleList(samples);
        }
    }

    public void searchByName() {
        view.printPrompt("검색어 > ");
        String keyword = scanner.nextLine();
        List<Sample> result = repository.findByNameContaining(keyword);
        if (result.isEmpty()) {
            view.printEmpty();
        } else {
            view.printSampleList(result);
        }
    }

    public void handleSubMenu() {
        while (true) {
            view.printMenu();
            int choice = readMenuChoice();
            switch (choice) {
                case 1 -> { register();     pause(); }
                case 2 -> { listAll();      pause(); }
                case 3 -> { searchByName(); pause(); }
                case 0 -> { return; }
                default -> view.printError("올바른 번호를 입력해 주세요.");
            }
        }
    }

    private void pause() {
        view.printPause();
        scanner.nextLine();
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
