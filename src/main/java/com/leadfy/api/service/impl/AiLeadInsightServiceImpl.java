package com.leadfy.api.service.impl;

import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.response.AiLeadInsightResponse;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.service.AiLeadInsightContext;
import com.leadfy.api.service.AiLeadInsightContextBuilder;
import com.leadfy.api.service.AiLeadInsightResultNormalizer;
import com.leadfy.api.service.AiLeadInsightService;
import com.leadfy.api.service.NormalizedAiLeadInsight;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiLeadInsightServiceImpl implements AiLeadInsightService {

	private final AiLeadInsightContextBuilder contextBuilder;
	private final AiLeadInsightResultNormalizer resultNormalizer;
	private final List<AiClient> aiClients;

	public AiLeadInsightServiceImpl(
			AiLeadInsightContextBuilder contextBuilder,
			AiLeadInsightResultNormalizer resultNormalizer,
			List<AiClient> aiClients
	) {
		this.contextBuilder = contextBuilder;
		this.resultNormalizer = resultNormalizer;
		this.aiClients = List.copyOf(aiClients);
	}

	@Override
	public AiLeadInsightResponse generate(Long ownerId, Long leadId) {
		AiLeadInsightContext context = contextBuilder.build(ownerId, leadId);
		AiLeadInsightResult result = getAiClient().generateLeadInsight(context);
		return toResponse(result);
	}

	private AiClient getAiClient() {
		if (aiClients.isEmpty()) {
			throw new AiInsightsUnavailableException();
		}

		return aiClients.getFirst();
	}

	private AiLeadInsightResponse toResponse(AiLeadInsightResult result) {
		NormalizedAiLeadInsight insight = resultNormalizer.normalize(result);

		return new AiLeadInsightResponse(
				insight.priorityScore(),
				insight.summary(),
				insight.conversionSignals(),
				insight.riskSignals(),
				insight.nextBestAction(),
				insight.suggestedMessage(),
				insight.confidence(),
				Instant.now()
		);
	}
}
