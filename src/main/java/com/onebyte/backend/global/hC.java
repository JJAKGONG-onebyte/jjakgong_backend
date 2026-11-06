package com.focusstudy.backend.global;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class hC {
    @GetMapping("/api/health")
    public String health() {
        return "ok";
    }
}