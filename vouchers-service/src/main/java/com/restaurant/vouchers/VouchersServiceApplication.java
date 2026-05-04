package com.restaurant.vouchers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class VouchersServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VouchersServiceApplication.class, args);
    }
}
