package com.busmonitor.exception;

public class UserNotFoundException extends BusMonitorException {
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id, 404);
    }

    public UserNotFoundException(String username) {
        super("User not found with username: " + username, 404);
    }
}
