package com.leadfy.api.service;

import com.leadfy.api.dto.response.AiLeadInsightResponse;

public interface AiLeadInsightService {

	AiLeadInsightResponse generate(Long ownerId, Long leadId);
}
