package com.busmonitor.model;

import jakarta.persistence.*;
import lombok.Data;
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
    private String Tnumber;
    @OneToMany(mappedBy = "bus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SensorData> readings = new ArrayList<>();
}
