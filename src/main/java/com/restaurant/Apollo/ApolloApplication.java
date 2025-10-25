package com.restaurant.Apollo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.restaurant.Apollo")  // scanează repo-urile
@EntityScan(basePackages = "com.restaurant.Apollo")
public class ApolloApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApolloApplication.class, args);
	}

}
