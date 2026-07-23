package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from your DevOps practice app!");
        response.put("time", LocalDateTime.now().toString());
        response.put("status", "running");
        return response;
    }

    @GetMapping("/api/version")
    public Map<String, String> version() {
        Map<String, String> response = new HashMap<>();
        response.put("app", "devops-practice-app");
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        return response;
    }
}
