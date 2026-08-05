package com.leadfy.api.service.impl;

import com.leadfy.api.dto.response.LeadSourceConversionResponse;
import com.leadfy.api.dto.response.LeadStatusMetricResponse;
import com.leadfy.api.dto.response.MetricsOverviewResponse;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.projection.LeadSourceConversionProjection;
import com.leadfy.api.repository.projection.LeadStatusCountProjection;
import com.leadfy.api.service.MetricsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsServiceImpl implements MetricsService {

	private static final int METRIC_SCALE = 2;

	private final LeadRepository leadRepository;

	public MetricsServiceImpl(LeadRepository leadRepository) {
		this.leadRepository = leadRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public MetricsOverviewResponse overview(Long ownerId) {
		Long totalLeads = leadRepository.countByOwnerId(ownerId);
		Long closedLeads = leadRepository.countByOwnerIdAndStatus(ownerId, LeadStatus.CLOSED);
		Long lostLeads = leadRepository.countByOwnerIdAndStatus(ownerId, LeadStatus.LOST);
		Long openLeads = totalLeads - closedLeads - lostLeads;

		return new MetricsOverviewResponse(
				totalLeads,
				openLeads,
				closedLeads,
				lostLeads,
				percentage(closedLeads, totalLeads),
				normalizeAverageDays(leadRepository.averageDaysToCloseByOwnerId(ownerId)),
				buildStatusMetrics(ownerId),
				buildSourceConversionMetrics(ownerId)
		);
	}

	private List<LeadStatusMetricResponse> buildStatusMetrics(Long ownerId) {
		Map<LeadStatus, Long> totalsByStatus = new EnumMap<>(LeadStatus.class);

		leadRepository.countLeadsByStatus(ownerId)
				.forEach(projection -> totalsByStatus.put(projection.getStatus(), projection.getTotal()));

		return Arrays.stream(LeadStatus.values())
				.map(status -> new LeadStatusMetricResponse(status, totalsByStatus.getOrDefault(status, 0L)))
				.toList();
	}

	private List<LeadSourceConversionResponse> buildSourceConversionMetrics(Long ownerId) {
		Map<LeadSource, LeadSourceConversionProjection> metricsBySource = new EnumMap<>(LeadSource.class);

		leadRepository.countConversionBySource(ownerId, LeadStatus.CLOSED)
				.forEach(projection -> metricsBySource.put(projection.getSource(), projection));

		return Arrays.stream(LeadSource.values())
				.map(source -> toSourceConversionResponse(source, metricsBySource.get(source)))
				.toList();
	}

	private LeadSourceConversionResponse toSourceConversionResponse(
			LeadSource source,
			LeadSourceConversionProjection projection
	) {
		if (projection == null) {
			return new LeadSourceConversionResponse(source, 0L, 0L, percentage(0L, 0L));
		}

		return new LeadSourceConversionResponse(
				source,
				projection.getTotal(),
				projection.getClosedLeads(),
				percentage(projection.getClosedLeads(), projection.getTotal())
		);
	}

	private BigDecimal percentage(Long part, Long total) {
		if (total == 0) {
			return BigDecimal.ZERO.setScale(METRIC_SCALE, RoundingMode.HALF_UP);
		}

		return BigDecimal.valueOf(part)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(total), METRIC_SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizeAverageDays(BigDecimal averageDays) {
		if (averageDays == null) {
			return null;
		}

		return averageDays.setScale(METRIC_SCALE, RoundingMode.HALF_UP);
	}
}
