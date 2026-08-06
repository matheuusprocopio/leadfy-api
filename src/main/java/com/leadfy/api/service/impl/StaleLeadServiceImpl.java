package com.leadfy.api.service.impl;

import com.leadfy.api.entity.Lead;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.service.StaleLeadService;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaleLeadServiceImpl implements StaleLeadService {

	private static final Logger log = LoggerFactory.getLogger(StaleLeadServiceImpl.class);
	private static final EnumSet<LeadStatus> EXCLUDED_STATUSES = EnumSet.of(LeadStatus.CLOSED, LeadStatus.LOST);

	private final LeadRepository leadRepository;
	private final int thresholdDays;

	public StaleLeadServiceImpl(
			LeadRepository leadRepository,
			@Value("${leadfy.stale-lead.threshold-days:7}") int thresholdDays
	) {
		this.leadRepository = leadRepository;
		this.thresholdDays = thresholdDays;
	}

	@Override
	@Transactional
	public int flagStaleLeads() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(thresholdDays);

		List<Lead> eligibleLeads = leadRepository.findLeadsEligibleForStaleFlag(cutoff, EXCLUDED_STATUSES);
		eligibleLeads.forEach(Lead::markAsStale);

		log.info("Flagged {} lead(s) as stale (threshold: {} day(s))", eligibleLeads.size(), thresholdDays);

		return eligibleLeads.size();
	}
}
