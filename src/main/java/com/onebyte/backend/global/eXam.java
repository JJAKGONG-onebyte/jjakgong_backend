package com.focusstudy.backend.global;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class eXam {
    @GetMapping("/api/exam")
    public String exam() {
        return "ok";
    }
}