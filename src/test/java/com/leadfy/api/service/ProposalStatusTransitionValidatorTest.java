package com.leadfy.api.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leadfy.api.enums.ProposalStatus;
import com.leadfy.api.exception.InvalidProposalStatusTransitionException;
import org.junit.jupiter.api.Test;

class ProposalStatusTransitionValidatorTest {

	private final ProposalStatusTransitionValidator validator = new ProposalStatusTransitionValidator();

	@Test
	void validateShouldAllowSentToAcceptedOrRejected() {
		assertThatCode(() -> validator.validate(ProposalStatus.SENT, ProposalStatus.ACCEPTED))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(ProposalStatus.SENT, ProposalStatus.REJECTED))
				.doesNotThrowAnyException();
	}

	@Test
	void validateShouldAllowSameStatusAsNoOp() {
		assertThatCode(() -> validator.validate(ProposalStatus.SENT, ProposalStatus.SENT))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(ProposalStatus.ACCEPTED, ProposalStatus.ACCEPTED))
				.doesNotThrowAnyException();
	}

	@Test
	void validateShouldRejectTransitionsFromAccepted() {
		assertThatThrownBy(() -> validator.validate(ProposalStatus.ACCEPTED, ProposalStatus.REJECTED))
				.isInstanceOf(InvalidProposalStatusTransitionException.class)
				.hasMessage("Invalid proposal status transition from ACCEPTED to REJECTED");
	}

	@Test
	void validateShouldRejectTransitionsFromRejected() {
		assertThatThrownBy(() -> validator.validate(ProposalStatus.REJECTED, ProposalStatus.ACCEPTED))
				.isInstanceOf(InvalidProposalStatusTransitionException.class)
				.hasMessage("Invalid proposal status transition from REJECTED to ACCEPTED");
	}
}
