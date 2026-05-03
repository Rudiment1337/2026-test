package com.busmonitor.exception;

public class InvalidSensorDataException extends BusMonitorException {
    public InvalidSensorDataException(String message) {
        super(message, 400);
    }
}
