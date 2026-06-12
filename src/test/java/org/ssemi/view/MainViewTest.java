package org.ssemi.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MainViewTest {

    private MainView view;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        view = new MainView();
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private String output() {
        return outContent.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printMainMenu_구분선_63개_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        assertTrue(output().contains("=".repeat(63)));
    }

    @Test
    void printMainMenu_시스템명_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        assertTrue(output().contains("반도체 시료 생산주문관리"));
    }

    @Test
    void printMainMenu_시스템현황_텍스트_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        assertTrue(output().contains("시스템 현황"));
    }

    @Test
    void printMainMenu_타임스탬프_패턴_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        assertTrue(output().matches("(?s).*\\d{4}-\\d{2}-\\d{2}.*"));
    }

    @Test
    void printMainMenu_등록시료_수_포함() {
        view.printMainMenu(12, 2540L, 36L, 32L);
        assertTrue(output().contains("12종"));
    }

    @Test
    void printMainMenu_출재고_포함() {
        view.printMainMenu(12, 2540L, 36L, 32L);
        assertTrue(output().contains("2,540"));
    }

    @Test
    void printMainMenu_전체주문_포함() {
        view.printMainMenu(12, 2540L, 36L, 32L);
        assertTrue(output().contains("36건"));
    }

    @Test
    void printMainMenu_생산라인_포함() {
        view.printMainMenu(12, 2540L, 36L, 32L);
        assertTrue(output().contains("32건 대기"));
    }

    @Test
    void printMainMenu_6개_메뉴_모두_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        String out = output();
        assertTrue(out.contains("[1]"));
        assertTrue(out.contains("[2]"));
        assertTrue(out.contains("[3]"));
        assertTrue(out.contains("[4]"));
        assertTrue(out.contains("[5]"));
        assertTrue(out.contains("[6]"));
        assertTrue(out.contains("[0]"));
    }

    @Test
    void printMainMenu_선택_프롬프트_포함() {
        view.printMainMenu(3, 150L, 5L, 2L);
        assertTrue(output().contains("선택 >"));
    }

    @Test
    void printError_오류_태그_포함() {
        view.printError("테스트 오류");
        assertTrue(output().contains("[오류]"));
    }

    @Test
    void printGoodbye_텍스트_포함() {
        view.printGoodbye();
        assertTrue(output().contains("종료"));
    }
}
