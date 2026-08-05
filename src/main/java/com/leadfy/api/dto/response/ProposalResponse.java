package com.leadfy.api.dto.response;

import com.leadfy.api.enums.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProposalResponse(
		Long id,
		Long leadId,
		BigDecimal amount,
		ProposalStatus status,
		LocalDate sentAt,
		LocalDate respondedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
