package com.leadfy.api.dto.response;

import com.leadfy.api.enums.LeadStatus;

public record LeadStatusMetricResponse(
		LeadStatus status,
		Long total
) {
}
