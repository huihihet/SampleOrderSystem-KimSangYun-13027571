package org.ssemi.model.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonFileUtil {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 파일 미존재·빈 파일도 빈 리스트로 정상 처리 (예외 미발생 계약)
    public static <T> List<T> readList(Path filePath, Type type) {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || content.equals("[]")) {
                return new ArrayList<>();
            }
            List<T> result = GSON.fromJson(content, type);
            return result != null ? result : new ArrayList<>();
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("JSON 파싱 오류: " + filePath, e);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 오류: " + filePath, e);
        }
    }

    public static <T> void writeList(Path filePath, List<T> list) {
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = GSON.toJson(list);
            Files.writeString(filePath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("파일 쓰기 오류: " + filePath, e);
        }
    }
}
