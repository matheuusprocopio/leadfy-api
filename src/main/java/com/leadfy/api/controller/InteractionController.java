package com.leadfy.api.controller;

import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.UpdateInteractionRequest;
import com.leadfy.api.dto.response.InteractionResponse;
import com.leadfy.api.security.AuthenticatedUser;
import com.leadfy.api.service.InteractionService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leads/{leadId}/interactions")
@Tag(name = "Interactions")
@SecurityRequirement(name = "bearerAuth")
public class InteractionController {

	private final InteractionService interactionService;

	public InteractionController(InteractionService interactionService) {
		this.interactionService = interactionService;
	}

	@PostMapping
	@Operation(summary = "Create an interaction for a lead")
	public ResponseEntity<InteractionResponse> create(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@Valid @RequestBody CreateInteractionRequest request
	) {
		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(interactionService.create(authenticatedUser.getId(), leadId, request));
	}

	@GetMapping
	@Operation(summary = "List interactions from a lead")
	public ResponseEntity<List<InteractionResponse>> findAll(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId
	) {
		return ResponseEntity.ok(interactionService.findAll(authenticatedUser.getId(), leadId));
	}

	@GetMapping("/{interactionId}")
	@Operation(summary = "Find an interaction by id")
	public ResponseEntity<InteractionResponse> findById(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long interactionId
	) {
		return ResponseEntity.ok(interactionService.findById(authenticatedUser.getId(), leadId, interactionId));
	}

	@PutMapping("/{interactionId}")
	@Operation(summary = "Update an interaction")
	public ResponseEntity<InteractionResponse> update(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long interactionId,
			@Valid @RequestBody UpdateInteractionRequest request
	) {
		return ResponseEntity.ok(interactionService.update(authenticatedUser.getId(), leadId, interactionId, request));
	}

	@DeleteMapping("/{interactionId}")
	@Operation(summary = "Delete an interaction")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId,
			@PathVariable Long interactionId
	) {
		interactionService.delete(authenticatedUser.getId(), leadId, interactionId);
		return ResponseEntity.noContent().build();
	}
}
