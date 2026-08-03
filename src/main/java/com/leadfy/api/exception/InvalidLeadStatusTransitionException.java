package com.leadfy.api.exception;

import com.leadfy.api.enums.LeadStatus;

public class InvalidLeadStatusTransitionException extends RuntimeException {

	public InvalidLeadStatusTransitionException(LeadStatus currentStatus, LeadStatus nextStatus) {
		super("Invalid lead status transition from " + currentStatus + " to " + nextStatus);
	}
}
