package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner dropConstraint(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				System.out.println("DEBUG: Dropping database constraint _user_role_check if exists...");
				jdbcTemplate.execute("ALTER TABLE _user DROP CONSTRAINT IF EXISTS _user_role_check");
				System.out.println("DEBUG: Constraint dropped successfully.");
			} catch (Exception e) {
				System.err.println("DEBUG: Error dropping constraint (might not exist): " + e.getMessage());
			}
		};
	}

}
