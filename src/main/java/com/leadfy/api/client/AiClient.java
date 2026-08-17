package com.leadfy.api.client;

import com.leadfy.api.service.AiLeadInsightContext;

public interface AiClient {

	AiLeadInsightResult generateLeadInsight(AiLeadInsightContext context);
}
