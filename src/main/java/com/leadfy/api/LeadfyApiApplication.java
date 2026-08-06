package com.leadfy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LeadfyApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeadfyApiApplication.class, args);
	}
}
