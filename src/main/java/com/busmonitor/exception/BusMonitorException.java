package com.busmonitor.exception;

import lombok.Getter;

@Getter
public class BusMonitorException extends RuntimeException {
    private final int statusCode;

    public BusMonitorException(String message) {
        super(message);
        this.statusCode = 400;
    }
    public BusMonitorException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
