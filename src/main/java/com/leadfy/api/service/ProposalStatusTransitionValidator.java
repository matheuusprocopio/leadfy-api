package com.leadfy.api.service;

import com.leadfy.api.enums.ProposalStatus;
import com.leadfy.api.exception.InvalidProposalStatusTransitionException;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProposalStatusTransitionValidator {

	private static final Map<ProposalStatus, Set<ProposalStatus>> ALLOWED_TRANSITIONS = Map.of(
			ProposalStatus.SENT, Set.of(ProposalStatus.ACCEPTED, ProposalStatus.REJECTED),
			ProposalStatus.ACCEPTED, Set.of(),
			ProposalStatus.REJECTED, Set.of()
	);

	public void validate(ProposalStatus currentStatus, ProposalStatus nextStatus) {
		if (currentStatus == nextStatus) {
			return;
		}

		if (!ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(nextStatus)) {
			throw new InvalidProposalStatusTransitionException(currentStatus, nextStatus);
		}
	}
}
