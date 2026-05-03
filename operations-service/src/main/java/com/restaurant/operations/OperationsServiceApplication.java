package com.restaurant.operations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableScheduling
@EnableMethodSecurity
public class OperationsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OperationsServiceApplication.class, args);
    }
}
