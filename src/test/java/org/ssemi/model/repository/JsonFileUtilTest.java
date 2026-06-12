package org.ssemi.model.repository;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonFileUtilTest {

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    @Test
    void 빈_리스트_writeList_후_readList_빈_리스트_반환(@TempDir Path tempDir) {
        Path file = tempDir.resolve("empty.json");
        JsonFileUtil.writeList(file, List.of());

        List<String> result = JsonFileUtil.readList(file, STRING_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void 항목_있는_리스트_writeList_후_readList_동일_내용(@TempDir Path tempDir) {
        Path file = tempDir.resolve("data.json");
        List<String> original = List.of("alpha", "beta", "gamma");
        JsonFileUtil.writeList(file, original);

        List<String> result = JsonFileUtil.readList(file, STRING_LIST_TYPE);

        assertEquals(original, result);
    }

    @Test
    void 존재하지_않는_파일_readList_빈_리스트_반환(@TempDir Path tempDir) {
        Path file = tempDir.resolve("nonexistent.json");

        List<String> result = JsonFileUtil.readList(file, STRING_LIST_TYPE);

        assertTrue(result.isEmpty());
    }

    @Test
    void 부모_디렉토리_없는_경로에_writeList_자동_생성_후_저장_성공(@TempDir Path tempDir) {
        Path file = tempDir.resolve("subdir/nested/data.json");

        assertDoesNotThrow(() -> JsonFileUtil.writeList(file, List.of("test")));
        assertTrue(Files.exists(file));
    }

    @Test
    void 손상된_JSON_파일_readList_RuntimeException(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("broken.json");
        Files.writeString(file, "[{broken", StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () -> JsonFileUtil.readList(file, STRING_LIST_TYPE));
    }
}
