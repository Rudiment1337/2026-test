package com.busmonitor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SensorDataDTO {
    @NotNull(message = "busId is required")
    @Min(value = 1, message = "busId must be greater than 0")
    private Long busId;
    @NotNull(message = "timestamp is required")
    private LocalDateTime timestamp;

    @NotNull(message = "sensors list is required")
    @Valid
    private List<SensorReading> sensors;

    @Data
    public static class SensorReading {
        @NotNull(message = "sensor type is required")
        private String type;

        @NotNull(message = "sensor value is required")
        @Min(value = 0, message = "sensor value must be >= 0")
        private Double value;
    }
}
