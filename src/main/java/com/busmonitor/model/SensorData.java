package com.busmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "sens_data", indexes = {
    @Index(name = "id_bus_time", columnList = "bus_id, timestamp"),
    @Index(name = "id_timestamp", columnList = "timestamp")
})
@Data
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_id", nullable = false)
    private Bus bus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SensorType sensorType;

    @Column(nullable = false)
    private Double value;
    @Column(nullable = false)
    private LocalDateTime timestamp;
    private boolean anomaly;
}
