package com.example.pulse.partitionmanager;

import com.example.pulse.partitionmanager.config.PartitionMaintenanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(PartitionMaintenanceProperties.class)
@SpringBootApplication(scanBasePackages = "com.example.pulse")
public class PartitionManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartitionManagerApplication.class, args);
    }
}

