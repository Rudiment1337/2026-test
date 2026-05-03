package com.busmonitor.dto;

import lombok.Data;
import com.busmonitor.model.SensorData;
import java.time.LocalDateTime;

@Data
public class SensorDataResponseDTO {
    private Long id;
    private Long busId;
    private String sensorType;
    private Double value;
    private LocalDateTime timestamp;
    private boolean anomaly;

    public static SensorDataResponseDTO fromSensorData(SensorData sensorData) {
        SensorDataResponseDTO dto = new SensorDataResponseDTO();
        dto.setId(sensorData.getId());
        dto.setBusId(sensorData.getBus().getId());
        dto.setSensorType(sensorData.getSensorType().getType());
        dto.setValue(sensorData.getValue());
        dto.setTimestamp(sensorData.getTimestamp());
        dto.setAnomaly(sensorData.isAnomaly());
        return dto;
    }
}
