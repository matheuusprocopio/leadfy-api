package com.leadfy.api.controller;

import com.leadfy.api.dto.request.UpdateAiRecommendationFeedbackRequest;
import com.leadfy.api.dto.response.AiLeadRecommendationResponse;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.security.AuthenticatedUser;
import com.leadfy.api.service.AiLeadRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/recommendations")
@Tag(name = "AI Recommendations")
@SecurityRequirement(name = "bearerAuth")
public class AiLeadRecommendationController {

	private final AiLeadRecommendationService aiLeadRecommendationService;

	public AiLeadRecommendationController(AiLeadRecommendationService aiLeadRecommendationService) {
		this.aiLeadRecommendationService = aiLeadRecommendationService;
	}

	@GetMapping
	@Operation(summary = "List active AI lead recommendations")
	public ResponseEntity<PageResponse<AiLeadRecommendationResponse>> findActive(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PageableDefault(size = 10, sort = "priorityScore", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ResponseEntity.ok(aiLeadRecommendationService.findActive(authenticatedUser.getId(), pageable));
	}

	@PostMapping("/leads/{leadId}")
	@Operation(summary = "Generate and persist an AI recommendation for a lead")
	public ResponseEntity<AiLeadRecommendationResponse> generateForLead(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long leadId
	) {
		return ResponseEntity.ok(aiLeadRecommendationService.generateForLead(authenticatedUser.getId(), leadId));
	}

	@PatchMapping("/{recommendationId}/feedback")
	@Operation(summary = "Register human feedback for an AI recommendation")
	public ResponseEntity<AiLeadRecommendationResponse> updateFeedback(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable Long recommendationId,
			@Valid @RequestBody UpdateAiRecommendationFeedbackRequest request
	) {
		return ResponseEntity.ok(
				aiLeadRecommendationService.updateFeedback(authenticatedUser.getId(), recommendationId, request)
		);
	}
}
