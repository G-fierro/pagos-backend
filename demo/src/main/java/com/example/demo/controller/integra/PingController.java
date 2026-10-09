package com.example.demo.controller.integra;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/ping")
public class PingController {

    public record PingRequest(@NotBlank(message = "es obligatorio") String mensaje) {
    }

    public record PingResponse(String estado, String eco, Instant hora) {
    }

    @GetMapping
    public PingResponse ping() {
        return new PingResponse("ok", null, Instant.now());
    }

    @PostMapping("/eco")
    public PingResponse eco(@Valid @RequestBody PingRequest request) {
        return new PingResponse("ok", request.mensaje(), Instant.now());
    }
}
