package com.example.pulse.metrickingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.pulse")
public class MetricIngestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetricIngestApplication.class, args);
    }
}

