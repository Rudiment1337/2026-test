package com.busmonitor.exception;

public class BusNotFoundException extends BusMonitorException {
    public BusNotFoundException(Long id) {
        super("Bus not found with id: " + id, 404);
    }
}
