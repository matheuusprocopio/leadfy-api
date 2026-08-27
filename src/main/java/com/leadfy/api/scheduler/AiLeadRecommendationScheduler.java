package com.leadfy.api.scheduler;

import com.leadfy.api.service.AiLeadRecommendationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiLeadRecommendationScheduler {

	private final AiLeadRecommendationService aiLeadRecommendationService;
	private final int maxLeadsPerRun;

	public AiLeadRecommendationScheduler(
			AiLeadRecommendationService aiLeadRecommendationService,
			@Value("${leadfy.ai.recommendation.max-leads-per-run:25}") int maxLeadsPerRun
	) {
		this.aiLeadRecommendationService = aiLeadRecommendationService;
		this.maxLeadsPerRun = maxLeadsPerRun;
	}

	@Scheduled(cron = "${leadfy.ai.recommendation.cron:0 30 7 * * *}")
	public void refreshRecommendations() {
		aiLeadRecommendationService.refreshOpenLeadRecommendations(maxLeadsPerRun);
	}
}
