package com.leadfy.api.controller;

import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.dto.response.ProposalResponse;
import com.leadfy.api.security.AuthenticatedUser;
import com.leadfy.api.service.ProposalService;
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
@RequestMapping("/api/leads/{leadId}/proposals")
@Tag(name = "Proposals")
@SecurityRequirement(name = "bearerAuth")
public class ProposalController {

	private final ProposalService proposalService;

	public ProposalController(ProposalService proposalService) {
		this.proposalService = proposalService;
	}

	@PostMapping
	@Operation(summary = "Create a proposal for a lead")
	public ResponseEntity<ProposalResponse> create(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@Valid @RequestBody CreateProposalRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(proposalService.create(authenticatedUser.getId(), leadId, request));
	}

	@GetMapping
	@Operation(summary = "List proposals from a lead")
	public ResponseEntity<List<ProposalResponse>> findAll(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId
	) {
		return ResponseEntity.ok(proposalService.findAll(authenticatedUser.getId(), leadId));
	}

	@GetMapping("/{proposalId}")
	@Operation(summary = "Find a proposal by id")
	public ResponseEntity<ProposalResponse> findById(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long proposalId
	) {
		return ResponseEntity.ok(proposalService.findById(authenticatedUser.getId(), leadId, proposalId));
	}

	@PutMapping("/{proposalId}")
	@Operation(summary = "Update a proposal")
	public ResponseEntity<ProposalResponse> update(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long proposalId,
			@Valid @RequestBody UpdateProposalRequest request
	) {
		return ResponseEntity.ok(proposalService.update(authenticatedUser.getId(), leadId, proposalId, request));
	}

	@PatchMapping("/{proposalId}/status")
	@Operation(summary = "Update a proposal status")
	public ResponseEntity<ProposalResponse> updateStatus(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long proposalId,
			@Valid @RequestBody UpdateProposalStatusRequest request
	) {
		return ResponseEntity.ok(proposalService.updateStatus(authenticatedUser.getId(), leadId, proposalId, request));
	}

	@DeleteMapping("/{proposalId}")
	@Operation(summary = "Delete a proposal")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long proposalId
	) {
		proposalService.delete(authenticatedUser.getId(), leadId, proposalId);
		return ResponseEntity.noContent().build();
	}
}
