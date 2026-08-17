package com.leadfy.api.service;

import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.enums.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AiLeadInsightContext(
		String leadName,
		String company,
		LeadSource source,
		LeadStatus status,
		boolean staleLead,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime closedAt,
		String notes,
		List<RecentInteraction> recentInteractions,
		List<RecentProposal> recentProposals
) {
	public AiLeadInsightContext {
		recentInteractions = recentInteractions == null ? List.of() : List.copyOf(recentInteractions);
		recentProposals = recentProposals == null ? List.of() : List.copyOf(recentProposals);
	}

	public record RecentInteraction(
			InteractionType type,
			String description,
			LocalDateTime interactionDate
	) {
	}

	public record RecentProposal(
			BigDecimal amount,
			ProposalStatus status,
			LocalDate sentAt,
			LocalDate respondedAt
	) {
	}
}
