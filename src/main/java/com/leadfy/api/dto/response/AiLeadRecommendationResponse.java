package com.leadfy.api.dto.response;

import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import java.time.Instant;
import java.util.List;

public record AiLeadRecommendationResponse(
		Long id,
		Long leadId,
		String leadName,
		String company,
		LeadSource leadSource,
		LeadStatus leadStatus,
		boolean staleLead,
		Integer priorityScore,
		String summary,
		List<String> conversionSignals,
		List<String> riskSignals,
		String nextBestAction,
		String suggestedMessage,
		String confidence,
		AiRecommendationStatus status,
		Boolean useful,
		boolean active,
		Instant generatedAt,
		Instant reviewedAt
) {
	public AiLeadRecommendationResponse {
		conversionSignals = conversionSignals == null ? List.of() : List.copyOf(conversionSignals);
		riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
	}
}
