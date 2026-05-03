package com.busmonitor.service;

import com.busmonitor.model.SensorType;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ThresholdImportService {

    private final Map<String, Double> thresholds = new HashMap<>();

    @PostConstruct
    public void initDefaultThresholds() {
        for (SensorType type : SensorType.values()) {
            thresholds.put(type.getType(), type.getWarningThreshold());
        }
        log.info("Default thresholds loaded: {}", thresholds);
    }
    public void importThresholdsFromCSV(MultipartFile file) {
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(file.getInputStream()))
                .withSkipLines(1)
                .build()) {

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 2) {
                    log.warn("Invalid line");
                    continue;
                }
                String sensorType = line[0].trim();
                double threshold = Double.parseDouble(line[1].trim());
                thresholds.put(sensorType, threshold);
                log.info("Updated threshold for {}: {}", sensorType, threshold);
            }
            log.info("CSV import completed. Thresholds: {}", thresholds);
        } catch (Exception e) {
            log.error("Failed to import CSV: {}", e.getMessage());
            throw new RuntimeException("CSV import failed: " + e.getMessage());
        }
    }
    public double getThreshold(String sensorType) {
        return thresholds.getOrDefault(sensorType, 0.0);
    }
}
