package com.busmonitor.controller;
import com.busmonitor.dto.SensorDataResponseDTO;
import com.busmonitor.dto.SensorDataDTO;
import com.busmonitor.model.Bus;
import com.busmonitor.model.SensorData;
import com.busmonitor.model.SensorType;
import com.busmonitor.repository.BusRepository;
import com.busmonitor.repository.SensorDataRepository;
import com.busmonitor.telegram.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sens")
public class SensorController {

    @Autowired
    private TelegramBotService telegramBotService;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private BusRepository busRepository;

    @PostMapping("/batch")
    public ResponseEntity<Map<String, String>> receiveBatchData(@RequestBody SensorDataDTO data) {
        log.info("Received data for busId: {}", data.getBusId());

        // Поиск или создание автобуса
        Bus bus = busRepository.findById(data.getBusId())
            .orElseGet(() -> {
                Bus newBus = new Bus();
                newBus.setId(data.getBusId());
                newBus.setLicensePlate("BUS-" + data.getBusId());
                newBus.setModel("Unknown");
                return busRepository.save(newBus);
            });

        // Сохранение данных
        int savedCount = 0;
        for (SensorDataDTO.SensorReading reading : data.getSensors()) {
            try {
                SensorData sensorData = new SensorData();
                sensorData.setBus(bus);
                SensorType sensorType = SensorType.valueOf(reading.getType());
                sensorData.setSensorType(sensorType);
                sensorData.setValue(reading.getValue());
                sensorData.setTimestamp(data.getTimestamp());
                sensorData.setAnomaly(reading.getValue() > sensorType.getWarningThreshold());
                sensorDataRepository.save(sensorData);
                if (sensorData.isAnomaly()) {
                    telegramBotService.sendAnomalyAlert(
                       bus.getId(),
                       sensorType.getType(),
                       reading.getValue(),
                       data.getTimestamp().toString()
                    );
		}
                savedCount++;
            } catch (IllegalArgumentException e) {
                log.error("Invalid sensor type: {}", reading.getType());
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", String.format("Saved %d readings for bus %d", savedCount, bus.getId()));
        return ResponseEntity.ok(response);
    }
	// Получеине последних данных
    @GetMapping("/latest")
    public ResponseEntity<List<SensorDataResponseDTO>> getLatestReadings(@RequestParam Long busId) {
        log.info("Getting latest data: {}", busId);
        List<SensorDataResponseDTO> data = sensorDataRepository.findLatestReadingsByBus(busId).stream()
            .map(SensorDataResponseDTO::fromSensorData)
            .collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }
	// Получение истории
    @GetMapping("/history")
    public ResponseEntity<List<SensorDataResponseDTO>> getHistory(
            @RequestParam Long busId,
            @RequestParam String from,
            @RequestParam String to) {
        log.info("Getting history: {} from {} to {}", busId, from, to);
        LocalDateTime fromDate = LocalDateTime.parse(from);
        LocalDateTime toDate = LocalDateTime.parse(to);
        List<SensorDataResponseDTO> data = sensorDataRepository
             .findByBusIdAndTimestampBetweenOrderByTimestampDesc(busId, fromDate, toDate)
            .stream()
            .map(SensorDataResponseDTO::fromSensorData)
            .collect(Collectors.toList());
         return ResponseEntity.ok(data);
     }
	// Получение данных о аномалиях
    @GetMapping("/alerts")
    public ResponseEntity<List<SensorDataResponseDTO>> getAlerts() {
        log.info("Getting all anomalies");
        List<SensorDataResponseDTO> data = sensorDataRepository.findAllAnomalies().stream()
            .map(SensorDataResponseDTO::fromSensorData)
            .collect(Collectors.toList());
        return ResponseEntity.ok(data);
    }
}
