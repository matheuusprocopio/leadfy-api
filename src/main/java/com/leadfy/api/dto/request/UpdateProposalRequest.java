package com.leadfy.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProposalRequest(
		@NotNull
		@DecimalMin("0.01")
		@Digits(integer = 10, fraction = 2)
		BigDecimal amount,

		@NotNull
		@PastOrPresent
		LocalDate sentAt
) {
}
