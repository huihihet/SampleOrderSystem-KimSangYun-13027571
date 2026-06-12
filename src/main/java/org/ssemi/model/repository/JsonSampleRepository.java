package org.ssemi.model.repository;

import com.google.gson.reflect.TypeToken;
import org.ssemi.model.entity.Sample;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonSampleRepository implements SampleRepository {

    private static final Type LIST_TYPE = new TypeToken<List<Sample>>() {}.getType();

    private final Path filePath;

    public JsonSampleRepository(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Sample sample) {
        List<Sample> all = findAll();
        boolean duplicated = all.stream()
            .anyMatch(s -> s.getSampleId().equals(sample.getSampleId()));
        if (duplicated) {
            throw new IllegalArgumentException("이미 존재하는 시료 ID: " + sample.getSampleId());
        }
        all.add(sample);
        JsonFileUtil.writeList(filePath, all);
    }

    @Override
    public Optional<Sample> findById(String sampleId) {
        return findAll().stream()
            .filter(s -> s.getSampleId().equals(sampleId))
            .findFirst();
    }

    @Override
    public List<Sample> findAll() {
        return JsonFileUtil.readList(filePath, LIST_TYPE);
    }

    @Override
    public List<Sample> findByNameContaining(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        return findAll().stream()
            .filter(s -> s.getName().toLowerCase().contains(keyword.toLowerCase()))
            .collect(Collectors.toList());
    }

    @Override
    public void update(Sample sample) {
        List<Sample> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getSampleId().equals(sample.getSampleId())) {
                all.set(i, sample);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new NoSuchElementException("존재하지 않는 시료 ID: " + sample.getSampleId());
        }
        JsonFileUtil.writeList(filePath, all);
    }

    @Override
    public void deleteById(String sampleId) {
        List<Sample> all = findAll();
        List<Sample> remaining = all.stream()
            .filter(s -> !s.getSampleId().equals(sampleId))
            .collect(Collectors.toList());
        JsonFileUtil.writeList(filePath, remaining);
    }
}
