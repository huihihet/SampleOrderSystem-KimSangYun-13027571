package org.ssemi.app;

import org.ssemi.controller.MonitoringController;
import org.ssemi.controller.OrderController;
import org.ssemi.controller.ProductionLineController;
import org.ssemi.controller.ReleaseController;
import org.ssemi.controller.SampleController;
import org.ssemi.view.MainView;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Router {

    private final SampleController sampleController;
    private final OrderController orderController;
    private final MonitoringController monitoringController;
    private final ReleaseController releaseController;
    private final ProductionLineController productionLineController;
    private final MainView mainView;
    private final Scanner scanner;

    public Router(SampleController sampleController,
                  OrderController orderController,
                  MonitoringController monitoringController,
                  ReleaseController releaseController,
                  ProductionLineController productionLineController,
                  MainView mainView,
                  Scanner scanner) {
        this.sampleController         = sampleController;
        this.orderController          = orderController;
        this.monitoringController     = monitoringController;
        this.releaseController        = releaseController;
        this.productionLineController = productionLineController;
        this.mainView                 = mainView;
        this.scanner                  = scanner;
    }

    public void run() {
        while (true) {
            int  totalSamples    = monitoringController.getSampleCount();
            long totalStock      = monitoringController.getTotalStock();
            long totalOrders     = monitoringController.getOrderCount();
            long prodLineWaiting = productionLineController.getQueueWaitingCount();

            mainView.printMainMenu(totalSamples, totalStock, totalOrders, prodLineWaiting);

            int choice = readMenuChoice();
            if (!route(choice)) break;
        }
        mainView.printGoodbye();
    }

    public boolean route(int menu) {
        switch (menu) {
            case 1 -> sampleController.handleSubMenu();
            case 2 -> orderController.placeOrder();
            case 3 -> orderController.handleSubMenu();
            case 4 -> showMonitoringLive();
            case 5 -> productionLineController.handleSubMenu();
            case 6 -> releaseController.processRelease();
            case 0 -> { return false; }
            default -> mainView.printError("올바른 번호를 입력해 주세요.");
        }
        return true;
    }

    private void showMonitoringLive() {
        monitoringController.showMonitoring(); // 첫 렌더는 호출 스레드에서 즉시 실행
        AtomicBoolean active = new AtomicBoolean(true);
        Thread t = new Thread(() -> {
            while (active.get()) {
                try { Thread.sleep(3000); } catch (InterruptedException e) { break; }
                if (active.get()) monitoringController.showMonitoring();
            }
        }, "monitoring-refresh");
        t.setDaemon(true);
        t.start();
        try { scanner.nextLine(); } catch (NoSuchElementException ignored) {}
        active.set(false);
        t.interrupt();
        try { t.join(300); } catch (InterruptedException ignored) {}
    }

    private int readMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
