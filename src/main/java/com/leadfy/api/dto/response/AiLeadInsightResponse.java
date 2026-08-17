package com.leadfy.api.dto.response;

import java.time.Instant;
import java.util.List;

public record AiLeadInsightResponse(
		Integer priorityScore,
		String summary,
		List<String> conversionSignals,
		List<String> riskSignals,
		String nextBestAction,
		String suggestedMessage,
		String confidence,
		Instant generatedAt
) {
	public AiLeadInsightResponse {
		conversionSignals = conversionSignals == null ? List.of() : List.copyOf(conversionSignals);
		riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
	}
}
