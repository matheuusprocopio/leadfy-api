package com.leadfy.api.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.request.UpdateAiRecommendationFeedbackRequest;
import com.leadfy.api.dto.response.AiLeadRecommendationResponse;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.entity.AiLeadRecommendation;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.AiClientException;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.AiResponseParsingException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.AiLeadRecommendationRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.service.AiLeadInsightContext;
import com.leadfy.api.service.AiLeadInsightContextBuilder;
import com.leadfy.api.service.AiLeadInsightResultNormalizer;
import com.leadfy.api.service.AiLeadRecommendationService;
import com.leadfy.api.service.NormalizedAiLeadInsight;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiLeadRecommendationServiceImpl implements AiLeadRecommendationService {

	private static final int RECOMMENDATION_FRESHNESS_DAYS = 1;
	private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
	};

	private final LeadRepository leadRepository;
	private final AiLeadRecommendationRepository aiLeadRecommendationRepository;
	private final AiLeadInsightContextBuilder contextBuilder;
	private final AiLeadInsightResultNormalizer resultNormalizer;
	private final ObjectMapper objectMapper;
	private final List<AiClient> aiClients;

	public AiLeadRecommendationServiceImpl(
			LeadRepository leadRepository,
			AiLeadRecommendationRepository aiLeadRecommendationRepository,
			AiLeadInsightContextBuilder contextBuilder,
			AiLeadInsightResultNormalizer resultNormalizer,
			ObjectMapper objectMapper,
			List<AiClient> aiClients
	) {
		this.leadRepository = leadRepository;
		this.aiLeadRecommendationRepository = aiLeadRecommendationRepository;
		this.contextBuilder = contextBuilder;
		this.resultNormalizer = resultNormalizer;
		this.objectMapper = objectMapper;
		this.aiClients = List.copyOf(aiClients);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<AiLeadRecommendationResponse> findActive(Long ownerId, Pageable pageable) {
		return PageResponse.from(
				aiLeadRecommendationRepository.findByOwnerIdAndActiveTrue(ownerId, pageable)
						.map(this::toResponse)
		);
	}

	@Override
	@Transactional
	public AiLeadRecommendationResponse generateForLead(Long ownerId, Long leadId) {
		Lead lead = leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));

		return toResponse(generateForLead(lead));
	}

	@Override
	@Transactional
	public AiLeadRecommendationResponse updateFeedback(
			Long ownerId,
			Long recommendationId,
			UpdateAiRecommendationFeedbackRequest request
	) {
		AiLeadRecommendation recommendation = aiLeadRecommendationRepository
				.findByIdAndOwnerId(recommendationId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("AI recommendation", recommendationId));

		recommendation.updateFeedback(request.status(), request.useful());
		return toResponse(recommendation);
	}

	@Override
	@Transactional
	public int refreshOpenLeadRecommendations(int maxLeads) {
		if (maxLeads <= 0) {
			return 0;
		}

		Instant freshAfter = Instant.now().minus(Duration.ofDays(RECOMMENDATION_FRESHNESS_DAYS));
		List<Lead> leads = leadRepository.findLeadsEligibleForAiRecommendations(
				freshAfter,
				List.of(LeadStatus.CLOSED, LeadStatus.LOST),
				PageRequest.of(0, maxLeads)
		);

		int refreshed = 0;
		for (Lead lead : leads) {
			try {
				generateForLead(lead);
				refreshed++;
			} catch (AiInsightsUnavailableException exception) {
				return refreshed;
			} catch (AiClientException | AiResponseParsingException exception) {
				// A single invalid provider response should not stop recommendations for other leads.
			}
		}

		return refreshed;
	}

	private AiLeadRecommendation generateForLead(Lead lead) {
		AiLeadInsightContext context = contextBuilder.build(lead);
		AiLeadInsightResult result = getAiClient().generateLeadInsight(context);
		NormalizedAiLeadInsight insight = resultNormalizer.normalize(result);

		aiLeadRecommendationRepository.deactivateActiveRecommendationsForLead(lead.getId());
		return aiLeadRecommendationRepository.save(new AiLeadRecommendation(
				lead,
				toJson(insight.conversionSignals()),
				toJson(insight.riskSignals()),
				insight,
				Instant.now()
		));
	}

	private AiClient getAiClient() {
		if (aiClients.isEmpty()) {
			throw new AiInsightsUnavailableException();
		}

		return aiClients.getFirst();
	}

	private AiLeadRecommendationResponse toResponse(AiLeadRecommendation recommendation) {
		Lead lead = recommendation.getLead();
		return new AiLeadRecommendationResponse(
				recommendation.getId(),
				lead.getId(),
				lead.getName(),
				lead.getCompany(),
				lead.getSource(),
				lead.getStatus(),
				lead.isStaleLead(),
				recommendation.getPriorityScore(),
				recommendation.getSummary(),
				fromJson(recommendation.getConversionSignalsJson()),
				fromJson(recommendation.getRiskSignalsJson()),
				recommendation.getNextBestAction(),
				recommendation.getSuggestedMessage(),
				recommendation.getConfidence(),
				recommendation.getStatus(),
				recommendation.getUseful(),
				recommendation.isActive(),
				recommendation.getGeneratedAt(),
				recommendation.getReviewedAt()
		);
	}

	private String toJson(List<String> values) {
		try {
			return objectMapper.writeValueAsString(values == null ? List.of() : values);
		} catch (JsonProcessingException exception) {
			throw new AiResponseParsingException(exception);
		}
	}

	private List<String> fromJson(String value) {
		try {
			return objectMapper.readValue(value, STRING_LIST_TYPE);
		} catch (JsonProcessingException exception) {
			throw new AiResponseParsingException(exception);
		}
	}
}
