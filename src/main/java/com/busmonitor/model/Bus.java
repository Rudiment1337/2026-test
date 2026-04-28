package com.busmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "buses")
@Data
public class Bus {
    @Id
    private Long id;

    @Column(nullable = false)
    private String model;

    @Column(unique = true)
    private String licensePlate;
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SensorData> readings = new ArrayList<>();
}
