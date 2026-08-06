package com.leadfy.api;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

// Started once and never stopped so every IT class reuses the same instance and port; without
// this, JUnit's per-class @Container lifecycle restarts the container between classes while
// Spring's context cache keeps reusing a DataSource wired to the now-dead previous container.
public abstract class AbstractIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

	static {
		POSTGRES.start();
	}
}
