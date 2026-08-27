package com.leadfy.api.service;

import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AiLeadInsightContextBuilder {

	private static final int RECENT_INTERACTIONS_LIMIT = 5;
	private static final int RECENT_PROPOSALS_LIMIT = 5;
	private static final int NOTES_MAX_LENGTH = 700;
	private static final int INTERACTION_DESCRIPTION_MAX_LENGTH = 500;
	private static final int GENERATED_TEXT_MAX_LENGTH = 1200;

	private final LeadRepository leadRepository;
	private final InteractionRepository interactionRepository;
	private final ProposalRepository proposalRepository;

	public AiLeadInsightContextBuilder(
			LeadRepository leadRepository,
			InteractionRepository interactionRepository,
			ProposalRepository proposalRepository
	) {
		this.leadRepository = leadRepository;
		this.interactionRepository = interactionRepository;
		this.proposalRepository = proposalRepository;
	}

	public AiLeadInsightContext build(Long ownerId, Long leadId) {
		Lead lead = leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));

		return build(lead);
	}

	public AiLeadInsightContext build(Lead lead) {
		Long leadId = lead.getId();
		Long ownerId = lead.getOwner().getId();

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
}
