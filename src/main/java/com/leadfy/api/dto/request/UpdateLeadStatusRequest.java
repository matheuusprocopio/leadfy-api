package com.leadfy.api.dto.request;

import com.leadfy.api.enums.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateLeadStatusRequest(
		@NotNull
		LeadStatus status
) {
}
