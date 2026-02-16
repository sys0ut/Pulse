package com.example.pulse.logingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.pulse")
public class LogIngestApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogIngestApplication.class, args);
    }
}

