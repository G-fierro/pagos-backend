package com.banduairlines.controller.integra;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    public record HealthResponse(String estado, Instant hora) {
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("ok", Instant.now());
    }
}
