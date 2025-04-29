package com.healthmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HealthManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthManagementApplication.class, args);
	}
}