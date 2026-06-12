package org.ssemi.model.repository;

import org.ssemi.model.entity.Sample;

import java.util.List;
import java.util.Optional;

public interface SampleRepository {
    void save(Sample sample);
    Optional<Sample> findById(String sampleId);
    List<Sample> findAll();
    List<Sample> findByNameContaining(String keyword);
    void update(Sample sample);
    void deleteById(String sampleId);
}
