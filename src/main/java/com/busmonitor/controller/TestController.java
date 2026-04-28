package com.busmonitor.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/ping")
    public Map<String, String> ping() {
        log.info("Health check endpoint");
        return Map.of("status", "OK", "message", "Bus System is running");
    }
}
