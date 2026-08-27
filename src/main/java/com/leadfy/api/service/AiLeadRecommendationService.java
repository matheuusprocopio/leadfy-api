package com.leadfy.api.service;

import com.leadfy.api.dto.request.UpdateAiRecommendationFeedbackRequest;
import com.leadfy.api.dto.response.AiLeadRecommendationResponse;
import com.leadfy.api.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface AiLeadRecommendationService {

	PageResponse<AiLeadRecommendationResponse> findActive(Long ownerId, Pageable pageable);

	AiLeadRecommendationResponse generateForLead(Long ownerId, Long leadId);

	AiLeadRecommendationResponse updateFeedback(
			Long ownerId,
			Long recommendationId,
			UpdateAiRecommendationFeedbackRequest request
	);

	int refreshOpenLeadRecommendations(int maxLeads);
}
