package com.leadfy.api.dto.response;

import com.leadfy.api.enums.InteractionType;
import java.time.LocalDateTime;

public record InteractionResponse(
		Long id,
		Long leadId,
		InteractionType type,
		String description,
		LocalDateTime interactionDate,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
