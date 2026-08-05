package com.leadfy.api.controller;

import com.leadfy.api.dto.response.MetricsOverviewResponse;
import com.leadfy.api.security.AuthenticatedUser;
import com.leadfy.api.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Metrics")
@SecurityRequirement(name = "bearerAuth")
public class MetricsController {

	private final MetricsService metricsService;

	public MetricsController(MetricsService metricsService) {
		this.metricsService = metricsService;
	}

	@GetMapping("/overview")
	@Operation(summary = "Get lead funnel metrics overview")
	public ResponseEntity<MetricsOverviewResponse> overview(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ResponseEntity.ok(metricsService.overview(authenticatedUser.getId()));
	}
}
