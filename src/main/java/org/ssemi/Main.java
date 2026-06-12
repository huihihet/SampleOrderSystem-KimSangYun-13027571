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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
        ReleaseController        releaseCtrl  = new ReleaseController(sampleRepo, orderRepo, releaseView, scanner);

        new Router(sampleCtrl, orderCtrl, monitorCtrl, releaseCtrl, prodLineCtrl, mainView, scanner).run();
    }

    private static void spawnNewWindow() throws Exception {
        String javaExe = ProcessHandle.current().info().command().orElse("java");
        String classpath = System.getProperty("java.class.path");

        // deleteOnExit() 사용 금지: Gradle JVM이 즉시 종료되면서 배치 파일이
        // 새 CMD 창 실행 전에 삭제되므로, 직접 정리하지 않고 OS 임시 폴더에 위탁
        Path bat = Files.createTempFile("ssemi-", ".bat");

        // 배치 파일: chcp 65001로 UTF-8 코드 페이지 설정 후 java 실행
        String script = "@echo off\r\n"
            + "title S-Semi 생산주문관리시스템\r\n"
            + "chcp 65001 >nul\r\n"
            + "\"" + javaExe + "\""
            + " -Dfile.encoding=UTF-8"
            + " -Dstdout.encoding=UTF-8"
            + " -Dstderr.encoding=UTF-8"
            + " -Dssemi.child=true"
            + " -cp \"" + classpath + "\""
            + " org.ssemi.Main\r\n";

        // 배치 파일은 시스템 기본 인코딩(CP949)으로 저장해야 cmd.exe가 한글 경로를 인식
        Files.write(bat, script.getBytes(Charset.defaultCharset()));

        // start "" "path.bat" : 빈 타이틀로 새 CMD 창에서 배치 파일 실행
        // /k 대신 start가 .bat 파일을 직접 실행 — ProcessBuilder 이중 인용 문제 우회
        new ProcessBuilder("cmd.exe", "/c", "start", "", bat.toAbsolutePath().toString())
            .start();
    }
}
