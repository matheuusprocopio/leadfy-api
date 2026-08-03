package com.leadfy.api.dto.response;

import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import java.time.LocalDateTime;

public record LeadResponse(
		Long id,
		String name,
		String company,
		String email,
		String phone,
		LeadSource source,
		LeadStatus status,
		String notes,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		LocalDateTime closedAt
) {
}
