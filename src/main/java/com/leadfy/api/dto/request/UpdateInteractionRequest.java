package com.leadfy.api.dto.request;

import com.leadfy.api.enums.InteractionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateInteractionRequest(
		@NotNull
		InteractionType type,

		@NotBlank
		@Size(max = 1000)
		String description,

		@NotNull
		@PastOrPresent
		LocalDateTime interactionDate
) {
}
