package com.leadfy.api.exception;

import com.leadfy.api.enums.ProposalStatus;

public class InvalidProposalStatusTransitionException extends RuntimeException {

	public InvalidProposalStatusTransitionException(ProposalStatus currentStatus, ProposalStatus nextStatus) {
		super("Invalid proposal status transition from " + currentStatus + " to " + nextStatus);
	}
}
