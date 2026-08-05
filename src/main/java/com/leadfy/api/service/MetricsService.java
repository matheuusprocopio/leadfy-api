package com.leadfy.api.service;

import com.leadfy.api.dto.response.MetricsOverviewResponse;

public interface MetricsService {

	MetricsOverviewResponse overview(Long ownerId);
}
