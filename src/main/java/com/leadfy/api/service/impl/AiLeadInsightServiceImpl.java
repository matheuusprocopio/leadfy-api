package com.leadfy.api.service.impl;

import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.response.AiLeadInsightResponse;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.AiResponseParsingException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import com.leadfy.api.service.AiLeadInsightContext;
import com.leadfy.api.service.AiLeadInsightService;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AiLeadInsightServiceImpl implements AiLeadInsightService {

	private static final int RECENT_INTERACTIONS_LIMIT = 5;
	private static final int RECENT_PROPOSALS_LIMIT = 5;
	private static final int NOTES_MAX_LENGTH = 700;
	private static final int INTERACTION_DESCRIPTION_MAX_LENGTH = 500;
	private static final int GENERATED_TEXT_MAX_LENGTH = 1200;

	private final LeadRepository leadRepository;
	private final InteractionRepository interactionRepository;
	private final ProposalRepository proposalRepository;
	private final List<AiClient> aiClients;

	public AiLeadInsightServiceImpl(
			LeadRepository leadRepository,
			InteractionRepository interactionRepository,
			ProposalRepository proposalRepository,
			List<AiClient> aiClients
	) {
		this.leadRepository = leadRepository;
		this.interactionRepository = interactionRepository;
		this.proposalRepository = proposalRepository;
		this.aiClients = List.copyOf(aiClients);
	}

	@Override
	public AiLeadInsightResponse generate(Long ownerId, Long leadId) {
		AiLeadInsightContext context = buildContext(ownerId, leadId);
		AiLeadInsightResult result = getAiClient().generateLeadInsight(context);
		return toResponse(result);
	}

	private AiLeadInsightContext buildContext(Long ownerId, Long leadId) {
		Lead lead = leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));

		List<AiLeadInsightContext.RecentInteraction> recentInteractions = interactionRepository
				.findByLeadIdAndLeadOwnerId(
						leadId,
						ownerId,
						PageRequest.of(
								0,
								RECENT_INTERACTIONS_LIMIT,
								Sort.by(Sort.Direction.DESC, "interactionDate")
						)
				)
				.map(this::toRecentInteraction)
				.toList();

		List<AiLeadInsightContext.RecentProposal> recentProposals = proposalRepository
				.findByLeadIdAndLeadOwnerId(
						leadId,
						ownerId,
						PageRequest.of(
								0,
								RECENT_PROPOSALS_LIMIT,
								Sort.by(Sort.Direction.DESC, "sentAt")
						)
				)
				.map(this::toRecentProposal)
				.toList();

		return new AiLeadInsightContext(
				cleanText(lead.getName(), GENERATED_TEXT_MAX_LENGTH),
				cleanText(lead.getCompany(), GENERATED_TEXT_MAX_LENGTH),
				lead.getSource(),
				lead.getStatus(),
				lead.isStaleLead(),
				lead.getCreatedAt(),
				lead.getUpdatedAt(),
				lead.getClosedAt(),
				cleanText(lead.getNotes(), NOTES_MAX_LENGTH),
				recentInteractions,
				recentProposals
		);
	}

	private AiLeadInsightContext.RecentInteraction toRecentInteraction(Interaction interaction) {
		return new AiLeadInsightContext.RecentInteraction(
				interaction.getType(),
				cleanText(interaction.getDescription(), INTERACTION_DESCRIPTION_MAX_LENGTH),
				interaction.getInteractionDate()
		);
	}

	private AiLeadInsightContext.RecentProposal toRecentProposal(Proposal proposal) {
		return new AiLeadInsightContext.RecentProposal(
				proposal.getAmount(),
				proposal.getStatus(),
				proposal.getSentAt(),
				proposal.getRespondedAt()
		);
	}

	private AiClient getAiClient() {
		if (aiClients.isEmpty()) {
			throw new AiInsightsUnavailableException();
		}

		return aiClients.getFirst();
	}

	private AiLeadInsightResponse toResponse(AiLeadInsightResult result) {
		if (result == null) {
			throw invalidAiResponse("AI response body is empty");
		}

		return new AiLeadInsightResponse(
				validatePriorityScore(result.priorityScore()),
				requireGeneratedText(result.summary(), "summary"),
				cleanGeneratedList(result.conversionSignals()),
				cleanGeneratedList(result.riskSignals()),
				requireGeneratedText(result.nextBestAction(), "nextBestAction"),
				requireGeneratedText(result.suggestedMessage(), "suggestedMessage"),
				validateConfidence(result.confidence()),
				Instant.now()
		);
	}

	private Integer validatePriorityScore(Integer priorityScore) {
		if (priorityScore == null || priorityScore < 0 || priorityScore > 100) {
			throw invalidAiResponse("priorityScore must be between 0 and 100");
		}

		return priorityScore;
	}

	private String validateConfidence(String confidence) {
		String normalizedConfidence = requireGeneratedText(confidence, "confidence").toUpperCase(Locale.ROOT);

		if (!List.of("LOW", "MEDIUM", "HIGH").contains(normalizedConfidence)) {
			throw invalidAiResponse("confidence must be LOW, MEDIUM or HIGH");
		}

		return normalizedConfidence;
	}

	private List<String> cleanGeneratedList(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return values.stream()
				.map(value -> cleanText(value, GENERATED_TEXT_MAX_LENGTH))
				.filter(value -> value != null && !value.isBlank())
				.toList();
	}

	private String requireGeneratedText(String value, String fieldName) {
		String cleanedValue = cleanText(value, GENERATED_TEXT_MAX_LENGTH);

		if (cleanedValue == null) {
			throw invalidAiResponse(fieldName + " is required");
		}

		return cleanedValue;
	}

	private String cleanText(String value, int maxLength) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		String normalized = value.trim().replaceAll("\\s+", " ");

		if (normalized.length() <= maxLength) {
			return normalized;
		}

		return normalized.substring(0, maxLength - 3) + "...";
	}

	private AiResponseParsingException invalidAiResponse(String message) {
		return new AiResponseParsingException(new IllegalArgumentException(message));
	}
}
