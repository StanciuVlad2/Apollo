package com.restaurant.cocktails;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
public class CocktailsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CocktailsServiceApplication.class, args);
    }
}
