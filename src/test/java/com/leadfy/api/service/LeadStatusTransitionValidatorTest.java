package com.leadfy.api.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.InvalidLeadStatusTransitionException;
import org.junit.jupiter.api.Test;

class LeadStatusTransitionValidatorTest {

	private final LeadStatusTransitionValidator validator = new LeadStatusTransitionValidator();

	@Test
	void validateShouldAllowRegularForwardTransitions() {
		assertThatCode(() -> validator.validate(LeadStatus.NEW, LeadStatus.CONTACT_MADE))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.CONTACT_MADE, LeadStatus.PROPOSAL_SENT))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.PROPOSAL_SENT, LeadStatus.NEGOTIATION))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.NEGOTIATION, LeadStatus.CLOSED))
				.doesNotThrowAnyException();
	}

	@Test
	void validateShouldAllowLostFromAnyNonFinalStatus() {
		assertThatCode(() -> validator.validate(LeadStatus.NEW, LeadStatus.LOST))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.CONTACT_MADE, LeadStatus.LOST))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.PROPOSAL_SENT, LeadStatus.LOST))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.NEGOTIATION, LeadStatus.LOST))
				.doesNotThrowAnyException();
	}

	@Test
	void validateShouldAllowSameStatusAsNoOp() {
		assertThatCode(() -> validator.validate(LeadStatus.NEW, LeadStatus.NEW))
				.doesNotThrowAnyException();
		assertThatCode(() -> validator.validate(LeadStatus.CLOSED, LeadStatus.CLOSED))
				.doesNotThrowAnyException();
	}

	@Test
	void validateShouldRejectSkippedSteps() {
		assertThatThrownBy(() -> validator.validate(LeadStatus.NEW, LeadStatus.CLOSED))
				.isInstanceOf(InvalidLeadStatusTransitionException.class)
				.hasMessage("Invalid lead status transition from NEW to CLOSED");
	}

	@Test
	void validateShouldRejectTransitionsFromClosed() {
		assertThatThrownBy(() -> validator.validate(LeadStatus.CLOSED, LeadStatus.LOST))
				.isInstanceOf(InvalidLeadStatusTransitionException.class)
				.hasMessage("Invalid lead status transition from CLOSED to LOST");
	}

	@Test
	void validateShouldRejectTransitionsFromLost() {
		assertThatThrownBy(() -> validator.validate(LeadStatus.LOST, LeadStatus.CONTACT_MADE))
				.isInstanceOf(InvalidLeadStatusTransitionException.class)
				.hasMessage("Invalid lead status transition from LOST to CONTACT_MADE");
	}
}
