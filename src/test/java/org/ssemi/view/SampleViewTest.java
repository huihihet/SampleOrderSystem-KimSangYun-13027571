package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssemi.model.entity.Sample;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleViewTest {

    private SampleView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
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

    @Test
    void printMenu_모든_항목_포함() {
        view.printMenu();
        String out = output();
        assertTrue(out.contains("[1]"), "메뉴 [1] 포함 확인");
        assertTrue(out.contains("[2]"), "메뉴 [2] 포함 확인");
        assertTrue(out.contains("[3]"), "메뉴 [3] 포함 확인");
        assertTrue(out.contains("[0]"), "메뉴 [0] 포함 확인");
    }

    @Test
    void printSampleList_단일_항목_헤더_구분선_행_출력() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 480);
        view.printSampleList(List.of(sample));
        String out = output();
        assertTrue(out.contains("S-001"), "ID 포함");
        assertTrue(out.contains("실리콘 웨이퍼-8인치"), "시료명 포함");
        assertTrue(out.contains("min/ea"), "단위 min/ea 포함");
        assertTrue(out.contains("ea"), "단위 ea 포함");
        assertTrue(out.contains("-".repeat(64)), "구분선 포함");
    }

    @Test
    void printSampleList_복수_항목_모든_행_출력() {
        List<Sample> samples = List.of(
            new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 480),
            new Sample("S-002", "GaAs 기판", 72, 0.85, 120)
        );
        view.printSampleList(samples);
        String out = output();
        assertTrue(out.contains("S-001"));
        assertTrue(out.contains("S-002"));
        assertTrue(out.contains("GaAs 기판"));
    }

    @Test
    void printSampleList_빈_리스트_예외_없음() {
        assertDoesNotThrow(() -> view.printSampleList(Collections.emptyList()));
    }

    @Test
    void printEmpty_없습니다_포함() {
        view.printEmpty();
        assertTrue(output().contains("없습니다"));
    }

    @Test
    void printSuccess_성공_태그_포함() {
        view.printSuccess("등록 완료");
        assertTrue(output().contains("[성공]"));
        assertTrue(output().contains("등록 완료"));
    }

    @Test
    void printError_오류_태그_포함() {
        view.printError("오류 메시지");
        assertTrue(output().contains("[오류]"));
        assertTrue(output().contains("오류 메시지"));
    }

    @Test
    void printPrompt_해당_문자열_출력() {
        view.printPrompt("선택 > ");
        assertTrue(output().contains("선택 > "));
    }
}
