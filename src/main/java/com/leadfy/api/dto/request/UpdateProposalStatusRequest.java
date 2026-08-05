package com.leadfy.api.dto.request;

import com.leadfy.api.enums.ProposalStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record UpdateProposalStatusRequest(
		@NotNull
		ProposalStatus status,

		@PastOrPresent
		LocalDate respondedAt
) {

	@AssertTrue(message = "respondedAt is required when status is ACCEPTED or REJECTED")
	public boolean isRespondedAtPresentForFinalStatus() {
		if (status == null || status == ProposalStatus.SENT) {
			return true;
		}

		return respondedAt != null;
	}

	@AssertTrue(message = "respondedAt must be null when status is SENT")
	public boolean isRespondedAtEmptyForSentStatus() {
		if (status == null || status != ProposalStatus.SENT) {
			return true;
		}

		return respondedAt == null;
	}
}
