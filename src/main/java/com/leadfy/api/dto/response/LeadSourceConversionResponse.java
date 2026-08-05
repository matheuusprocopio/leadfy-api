package com.leadfy.api.dto.response;

import com.leadfy.api.enums.LeadSource;
import java.math.BigDecimal;

public record LeadSourceConversionResponse(
		LeadSource source,
		Long totalLeads,
		Long closedLeads,
		BigDecimal conversionRatePercentage
) {
}
