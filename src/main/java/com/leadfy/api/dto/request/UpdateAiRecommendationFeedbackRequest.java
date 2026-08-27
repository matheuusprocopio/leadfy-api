package com.leadfy.api.dto.request;

import com.leadfy.api.enums.AiRecommendationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAiRecommendationFeedbackRequest(
		@NotNull
		AiRecommendationStatus status,

		Boolean useful
) {
}
