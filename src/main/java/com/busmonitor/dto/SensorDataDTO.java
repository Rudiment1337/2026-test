package com.busmonitor.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SensorDataDTO {
    private Long busId;
    private LocalDateTime timestamp;
    private List<SensorReading> sensors;

    @Data
    public static class SensorReading {
        private String type;
        private Double value;
    }
}
