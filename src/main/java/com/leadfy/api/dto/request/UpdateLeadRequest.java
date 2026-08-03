package com.leadfy.api.dto.request;

import com.leadfy.api.enums.LeadSource;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateLeadRequest(
		@NotBlank
		@Size(max = 120)
		String name,

		@Size(max = 120)
		String company,

		@Email
		@Size(max = 160)
		String email,

		@Size(max = 30)
		String phone,

		@NotNull
		LeadSource source,

		@Size(max = 1000)
		String notes
) {

	@AssertTrue(message = "email or phone must be provided")
	public boolean isContactProvided() {
		return hasText(email) || hasText(phone);
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}
}
