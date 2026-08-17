package com.leadfy.api.controller;

import com.leadfy.api.dto.response.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health")
public class HealthController {

	@GetMapping
	@Operation(summary = "Check API availability")
	public ResponseEntity<HealthResponse> check() {
		return ResponseEntity.ok(new HealthResponse("UP", "leadfy-api", Instant.now()));
	}
}
