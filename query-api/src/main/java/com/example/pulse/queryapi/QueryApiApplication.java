package com.example.pulse.queryapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.pulse")
public class QueryApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryApiApplication.class, args);
    }
}

