package com.busmonitor.dto;

import com.busmonitor.model.SensorType;
import lombok.Data;

@Data
public class AlertDTO {
    private SensorType sensorType;
    private Action action;

    public enum Action {
        ok, warning, error
    }
    public static AlertDTO fromSensorData(SensorType sensorType, Double value) {
        AlertDTO alert = new AlertDTO();
        alert.setSensorType(sensorType);
        if (sensorType == SensorType.fuel_level) {
            if (value <= sensorType.getErrorThreshold()) {
                alert.setAction(Action.error);
            } else if (value <= sensorType.getWarningThreshold()) {
                alert.setAction(Action.warning);
            } else {
                alert.setAction(Action.ok);
            }
        } else {
            if (value >= sensorType.getErrorThreshold()) {
                alert.setAction(Action.error);
            } else if (value >= sensorType.getWarningThreshold()) {
                alert.setAction(Action.warning);
            } else {
                alert.setAction(Action.ok);
            }
        }
        return alert;
    }
}
