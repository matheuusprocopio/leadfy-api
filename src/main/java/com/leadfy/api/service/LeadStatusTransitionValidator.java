package com.leadfy.api.service;

import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.InvalidLeadStatusTransitionException;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class LeadStatusTransitionValidator {

	private static final Map<LeadStatus, Set<LeadStatus>> ALLOWED_TRANSITIONS = Map.of(
			LeadStatus.NEW, Set.of(LeadStatus.CONTACT_MADE, LeadStatus.LOST),
			LeadStatus.CONTACT_MADE, Set.of(LeadStatus.PROPOSAL_SENT, LeadStatus.LOST),
			LeadStatus.PROPOSAL_SENT, Set.of(LeadStatus.NEGOTIATION, LeadStatus.LOST),
			LeadStatus.NEGOTIATION, Set.of(LeadStatus.CLOSED, LeadStatus.LOST),
			LeadStatus.CLOSED, Set.of(),
			LeadStatus.LOST, Set.of()
	);

	public void validate(LeadStatus currentStatus, LeadStatus nextStatus) {
		if (currentStatus == nextStatus) {
			return;
		}

		if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
			throw new InvalidLeadStatusTransitionException(currentStatus, nextStatus);
		}
	}
}
