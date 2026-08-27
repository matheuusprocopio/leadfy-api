package com.leadfy.api.service;

import java.util.List;

public record NormalizedAiLeadInsight(
		Integer priorityScore,
		String summary,
		List<String> conversionSignals,
		List<String> riskSignals,
		String nextBestAction,
		String suggestedMessage,
		String confidence
) {
	public NormalizedAiLeadInsight {
		conversionSignals = conversionSignals == null ? List.of() : List.copyOf(conversionSignals);
		riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
	}
}
