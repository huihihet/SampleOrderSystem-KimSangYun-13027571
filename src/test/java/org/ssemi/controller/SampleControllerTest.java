package org.ssemi.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Sample;
import org.ssemi.model.repository.JsonSampleRepository;
import org.ssemi.view.SampleView;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class SampleControllerTest {

    @TempDir
    Path tempDir;

    private JsonSampleRepository repository;
    private SampleView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        repository = new JsonSampleRepository(tempDir.resolve("samples.json"));
        view = new SampleView();
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String output() {
        return outContent.toString();
    }

    private SampleController controller(String input) {
        return new SampleController(repository, view, new Scanner(input));
    }

    @Test
    void register_정상_흐름_저장_및_성공_출력() {
        controller("S-001\n실리콘 웨이퍼\n30\n0.92\n100\n").register();

        assertTrue(output().contains("[성공]"), "성공 메시지 출력 확인");
        assertEquals(1, repository.findAll().size());
        assertEquals("실리콘 웨이퍼", repository.findById("S-001").get().getName());
    }

    @Test
    void register_중복_ID_오류_출력_저장_안됨() {
        repository.save(new Sample("S-001", "기존 시료", 30, 0.9, 100));
        outContent.reset();

        controller("S-001\n새 시료\n30\n0.9\n50\n").register();

        assertTrue(output().contains("[오류]"), "오류 메시지 출력 확인");
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void register_빈_이름_오류_출력_저장_안됨() {
        controller("S-001\n\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_공백만_이름_오류_출력_저장_안됨() {
        controller("S-001\n   \n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_avgProductionTime_파싱_실패_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\nabc\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_stock_파싱_실패_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\n30\n0.9\nxyz\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_yield_초과_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\n30\n1.5\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_yield_음수_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\n30\n-0.1\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_yield_경계값_0_0_정상_저장() {
        controller("S-001\n실리콘 웨이퍼\n30\n0.0\n100\n").register();

        assertTrue(output().contains("[성공]"));
        assertEquals(1, repository.findAll().size());
        assertEquals(0.0, repository.findById("S-001").get().getYield());
    }

    @Test
    void register_yield_경계값_1_0_정상_저장() {
        controller("S-001\n실리콘 웨이퍼\n30\n1.0\n100\n").register();

        assertTrue(output().contains("[성공]"));
        assertEquals(1, repository.findAll().size());
        assertEquals(1.0, repository.findById("S-001").get().getYield());
    }

    @Test
    void register_stock_음수_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\n30\n0.9\n-1\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void register_avgProductionTime_0_오류_출력() {
        controller("S-001\n실리콘 웨이퍼\n0\n").register();

        assertTrue(output().contains("[오류]"));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void listAll_빈_목록_없습니다_출력() {
        controller("").listAll();

        assertTrue(output().contains("없습니다"));
    }

    @Test
    void listAll_항목_있음_테이블_헤더_포함() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480));
        outContent.reset();

        controller("").listAll();

        String out = output();
        assertFalse(out.contains("[성공]"), "성공 태그 없음");
        assertTrue(out.contains("S-001"), "ID 포함");
        // 헤더 컬럼명 확인
        assertTrue(out.contains("ID") || out.contains("시료명"), "테이블 헤더 포함");
    }

    @Test
    void searchByName_일치_항목_있음_출력() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 480));
        repository.save(new Sample("S-002", "GaAs 기판", 72, 0.85, 120));
        outContent.reset();

        controller("웨이퍼\n").searchByName();

        String out = output();
        assertTrue(out.contains("S-001"));
        assertFalse(out.contains("GaAs 기판"));
    }

    @Test
    void searchByName_일치_없음_없습니다_출력() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480));
        outContent.reset();

        controller("ZZZ_없는키워드\n").searchByName();

        assertTrue(output().contains("없습니다"));
    }

    @Test
    void searchByName_빈_키워드_전체_목록_출력() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480));
        repository.save(new Sample("S-002", "GaAs 기판", 72, 0.85, 120));
        outContent.reset();

        controller("\n").searchByName();

        String out = output();
        assertTrue(out.contains("S-001"));
        assertTrue(out.contains("S-002"));
    }
}
