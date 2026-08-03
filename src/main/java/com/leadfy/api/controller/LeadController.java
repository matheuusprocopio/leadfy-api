package com.leadfy.api.controller;

import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.dto.response.LeadResponse;
import com.leadfy.api.security.AuthenticatedUser;
import com.leadfy.api.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads")
@Tag(name = "Leads")
@SecurityRequirement(name = "bearerAuth")
public class LeadController {

	private final LeadService leadService;

	public LeadController(LeadService leadService) {
		this.leadService = leadService;
	}

	@PostMapping
	@Operation(summary = "Create a lead")
	public ResponseEntity<LeadResponse> create(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody CreateLeadRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(leadService.create(authenticatedUser.getId(), request));
	}

	@GetMapping
	@Operation(summary = "List authenticated user's leads")
	public ResponseEntity<List<LeadResponse>> findAll(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser
	) {
		return ResponseEntity.ok(leadService.findAll(authenticatedUser.getId()));
	}

	@GetMapping("/{leadId}")
	@Operation(summary = "Find a lead by id")
	public ResponseEntity<LeadResponse> findById(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId
	) {
		return ResponseEntity.ok(leadService.findById(authenticatedUser.getId(), leadId));
	}

	@PutMapping("/{leadId}")
	@Operation(summary = "Update a lead")
	public ResponseEntity<LeadResponse> update(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@Valid @RequestBody UpdateLeadRequest request
	) {
		return ResponseEntity.ok(leadService.update(authenticatedUser.getId(), leadId, request));
	}

	@PatchMapping("/{leadId}/status")
	@Operation(summary = "Update a lead status")
	public ResponseEntity<LeadResponse> updateStatus(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@Valid @RequestBody UpdateLeadStatusRequest request
	) {
		return ResponseEntity.ok(leadService.updateStatus(authenticatedUser.getId(), leadId, request));
	}

	@DeleteMapping("/{leadId}")
	@Operation(summary = "Delete a lead")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId
	) {
		leadService.delete(authenticatedUser.getId(), leadId);
		return ResponseEntity.noContent().build();
	}
}
