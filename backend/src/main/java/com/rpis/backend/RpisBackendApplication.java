package com.rpis.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.rpis.backend.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RpisBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(RpisBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(UserService userService) {
		return args -> {
			System.out.println("Seeding database with default user accounts...");
			try {
				userService.seedUsers();
				System.out.println("Database seeded successfully!");
			} catch (Exception e) {
				System.err.println("Could not seed database: " + e.getMessage());
			}
		};
	}
}
