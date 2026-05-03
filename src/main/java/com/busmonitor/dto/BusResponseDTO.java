package com.busmonitor.dto;

import lombok.Data;
import com.busmonitor.model.Bus;

@Data
public class BusResponseDTO {
    private Long id;
    private String model;
    private String licensePlate;
    public static BusResponseDTO fromBus(Bus bus) {
        BusResponseDTO dto = new BusResponseDTO();
        dto.setId(bus.getId());
        dto.setModel(bus.getModel());
        dto.setLicensePlate(bus.getLicensePlate());
        return dto;
    }
}
