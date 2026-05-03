package com.busmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BusMonitoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(BusMonitoringApplication.class, args);
    }
}
