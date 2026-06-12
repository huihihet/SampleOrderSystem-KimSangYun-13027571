package org.ssemi;

import org.ssemi.app.Router;
import org.ssemi.controller.MonitoringController;
import org.ssemi.controller.OrderController;
import org.ssemi.controller.ProductionLineController;
import org.ssemi.controller.ReleaseController;
import org.ssemi.controller.SampleController;
import org.ssemi.model.repository.JsonOrderRepository;
import org.ssemi.model.repository.JsonProductionQueueRepository;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.model.repository.OrderRepository;
import org.ssemi.model.repository.ProductionQueueRepository;
import org.ssemi.model.repository.SampleRepository;
import org.ssemi.view.MainView;
import org.ssemi.view.MonitoringView;
import org.ssemi.view.OrderView;
import org.ssemi.view.ProductionLineView;
import org.ssemi.view.ReleaseView;
import org.ssemi.view.SampleView;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        if (System.getProperty("ssemi.child") == null) {
            spawnNewWindow();
            return;
        }
        // 새 창에서 직접 실행: stdout을 파일 디스크립터 기반 UTF-8 스트림으로 교체
        System.setOut(new PrintStream(new FileOutputStream(java.io.FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(java.io.FileDescriptor.err), true, StandardCharsets.UTF_8));
        Path samplesPath = Path.of("src/main/resources/data/samples.json");
        Path ordersPath  = Path.of("src/main/resources/data/orders.json");
        Path queuePath   = Path.of("src/main/resources/data/production_queue.json");

        Scanner scanner = new Scanner(System.in);

        SampleRepository          sampleRepo = new JsonSampleRepository(samplesPath);
        OrderRepository           orderRepo  = new JsonOrderRepository(ordersPath);
        ProductionQueueRepository queueRepo  = new JsonProductionQueueRepository(queuePath);

        MainView           mainView    = new MainView();
        SampleView         sampleView  = new SampleView();
        OrderView          orderView   = new OrderView();
        MonitoringView     monitorView = new MonitoringView();
        ReleaseView        releaseView = new ReleaseView();
        ProductionLineView prodView    = new ProductionLineView();

        SampleController         sampleCtrl   = new SampleController(sampleRepo, sampleView, scanner);
        ProductionLineController prodLineCtrl = new ProductionLineController(sampleRepo, orderRepo, queueRepo, prodView, scanner);
        OrderController          orderCtrl    = new OrderController(sampleRepo, orderRepo, prodLineCtrl, orderView, scanner);
        MonitoringController     monitorCtrl  = new MonitoringController(sampleRepo, orderRepo, monitorView);
        ReleaseController        releaseCtrl  = new ReleaseController(orderRepo, releaseView, scanner);

        new Router(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodLineCtrl, mainView, scanner).run();
    }

    private static void spawnNewWindow() throws Exception {
        String javaExe = ProcessHandle.current().info().command().orElse("java");
        String classpath = System.getProperty("java.class.path");
        new ProcessBuilder(
            "cmd.exe", "/c", "start", "S-Semi 생산주문관리시스템",
            "cmd.exe", "/k",
            javaExe,
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-Dssemi.child=true",
            "-cp", classpath,
            "org.ssemi.Main"
        ).start();
    }
}
