package org.ssemi.model.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ssemi.model.entity.Sample;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonSampleRepositoryTest {

    @TempDir
    Path tempDir;

    private Path filePath;
    private JsonSampleRepository repository;

    @BeforeEach
    void setUp() {
        filePath = tempDir.resolve("samples.json");
        repository = new JsonSampleRepository(filePath);
    }

    @Test
    void save_후_findById_조회_성공() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);
        repository.save(sample);

        Optional<Sample> found = repository.findById("S-001");

        assertTrue(found.isPresent());
        assertEquals("실리콘 웨이퍼", found.get().getName());
    }

    @Test
    void save_후_findAll_목록에_포함() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);
        repository.save(sample);

        List<Sample> all = repository.findAll();

        assertEquals(1, all.size());
    }

    @Test
    void 영속성_save_후_새_인스턴스_재조회() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);
        repository.save(sample);

        JsonSampleRepository newRepo = new JsonSampleRepository(filePath);
        Optional<Sample> found = newRepo.findById("S-001");

        assertTrue(found.isPresent());
        assertEquals("S-001", found.get().getSampleId());
        assertEquals(480, found.get().getStock());
    }

    @Test
    void 중복_sampleId_save_IllegalArgumentException() {
        Sample s1 = new Sample("S-001", "웨이퍼A", 30, 0.9, 100);
        Sample s2 = new Sample("S-001", "웨이퍼B", 45, 0.8, 200);
        repository.save(s1);

        assertThrows(IllegalArgumentException.class, () -> repository.save(s2));
    }

    @Test
    void update_기존_항목_수정_후_재조회() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);
        repository.save(sample);

        sample.setStock(300);
        repository.update(sample);

        Optional<Sample> found = repository.findById("S-001");
        assertTrue(found.isPresent());
        assertEquals(300, found.get().getStock());
    }

    @Test
    void update_존재하지_않는_ID_NoSuchElementException() {
        Sample sample = new Sample("S-999", "없는 시료", 30, 0.9, 100);

        assertThrows(java.util.NoSuchElementException.class, () -> repository.update(sample));
    }

    @Test
    void deleteById_삭제_후_findById_empty() {
        Sample sample = new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480);
        repository.save(sample);
        repository.deleteById("S-001");

        Optional<Sample> found = repository.findById("S-001");
        assertTrue(found.isEmpty());
    }

    @Test
    void findByNameContaining_이름_포함_항목만_반환() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼-8인치", 30, 0.92, 480));
        repository.save(new Sample("S-002", "GaAs 기판", 45, 0.85, 200));
        repository.save(new Sample("S-003", "실리콘 웨이퍼-12인치", 60, 0.88, 100));

        List<Sample> result = repository.findByNameContaining("웨이퍼");

        assertEquals(2, result.size());
    }

    @Test
    void findByNameContaining_blank_키워드_전체_반환() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480));
        repository.save(new Sample("S-002", "GaAs 기판", 45, 0.85, 200));

        List<Sample> result = repository.findByNameContaining("   ");

        assertEquals(2, result.size());
    }

    @Test
    void findByNameContaining_null_키워드_전체_반환() {
        repository.save(new Sample("S-001", "실리콘 웨이퍼", 30, 0.92, 480));
        repository.save(new Sample("S-002", "GaAs 기판", 45, 0.85, 200));

        List<Sample> result = repository.findByNameContaining(null);

        assertEquals(2, result.size());
    }

    @Test
    void findByNameContaining_대소문자_무관_검색() {
        repository.save(new Sample("S-001", "GaN-HEMT-A", 30, 0.85, 200));
        repository.save(new Sample("S-002", "SiC-MOSFET-B", 45, 0.72, 100));

        assertEquals(1, repository.findByNameContaining("gan").size());
        assertEquals(1, repository.findByNameContaining("GAN").size());
        assertEquals(1, repository.findByNameContaining("Gan").size());
        assertEquals(1, repository.findByNameContaining("mosfet").size());
    }

    @Test
    void findById_존재하지_않는_ID_Optional_empty() {
        Optional<Sample> found = repository.findById("S-999");

        assertTrue(found.isEmpty());
    }
}
