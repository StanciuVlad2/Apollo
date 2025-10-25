package com.restaurant.Apollo.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/salut")
public class HelloController {

    @GetMapping
    public ResponseEntity<String> sayHello(){
       return ResponseEntity.ok("Hello World");
    }
}
