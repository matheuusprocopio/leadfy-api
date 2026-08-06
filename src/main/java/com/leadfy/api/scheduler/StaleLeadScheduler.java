package com.leadfy.api.scheduler;

import com.leadfy.api.service.StaleLeadService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StaleLeadScheduler {

	private final StaleLeadService staleLeadService;

	public StaleLeadScheduler(StaleLeadService staleLeadService) {
		this.staleLeadService = staleLeadService;
	}

	@Scheduled(cron = "${leadfy.stale-lead.cron:0 0 2 * * *}")
	public void flagStaleLeads() {
		staleLeadService.flagStaleLeads();
	}
}
