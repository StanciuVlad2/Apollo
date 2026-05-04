package com.restaurant.vouchers.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "operations-service")
public interface OperationsClient {

    @GetMapping("/api/orders/{id}")
    Map<String, Object> getOrder(@PathVariable Long id);
}
