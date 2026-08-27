package com.leadfy.api.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MetricsOverviewResponse(
		Long totalLeads,
		Long openLeads,
		Long closedLeads,
		Long lostLeads,
		BigDecimal conversionRatePercentage,
		BigDecimal averageDaysToClose,
		Long aiRecommendedLeads,
		Long aiRecommendedClosedLeads,
		Long aiRecommendationActioned,
		Long aiRecommendationUseful,
		BigDecimal aiRecommendationConversionRatePercentage,
		BigDecimal aiRecommendationActionRatePercentage,
		List<LeadStatusMetricResponse> leadsByStatus,
		List<LeadSourceConversionResponse> conversionBySource
) {
}
