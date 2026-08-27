package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.dto.response.LeadSourceConversionResponse;
import com.leadfy.api.dto.response.LeadStatusMetricResponse;
import com.leadfy.api.dto.response.MetricsOverviewResponse;
import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.repository.AiLeadRecommendationRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.projection.LeadSourceConversionProjection;
import com.leadfy.api.repository.projection.LeadStatusCountProjection;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceImplTest {

	private static final Long OWNER_ID = 1L;

	@Mock
	private LeadRepository leadRepository;

	@Mock
	private AiLeadRecommendationRepository aiLeadRecommendationRepository;

	@InjectMocks
	private MetricsServiceImpl metricsService;

	@Test
	void overviewShouldReturnZeroMetricsWhenOwnerHasNoLeads() {
		when(leadRepository.countByOwnerId(OWNER_ID)).thenReturn(0L);
		when(leadRepository.countByOwnerIdAndStatus(OWNER_ID, LeadStatus.CLOSED)).thenReturn(0L);
		when(leadRepository.countByOwnerIdAndStatus(OWNER_ID, LeadStatus.LOST)).thenReturn(0L);
		when(leadRepository.countLeadsByStatus(OWNER_ID)).thenReturn(List.of());
		when(leadRepository.countConversionBySource(OWNER_ID, LeadStatus.CLOSED)).thenReturn(List.of());
		when(leadRepository.averageDaysToCloseByOwnerId(OWNER_ID)).thenReturn(null);
		when(aiLeadRecommendationRepository.countRecommendedLeadsByOwnerId(OWNER_ID)).thenReturn(0L);
		when(aiLeadRecommendationRepository.countRecommendedClosedLeadsByOwnerId(OWNER_ID, LeadStatus.CLOSED))
				.thenReturn(0L);
		when(aiLeadRecommendationRepository.countByOwnerIdAndStatus(OWNER_ID, AiRecommendationStatus.ACTIONED))
				.thenReturn(0L);
		when(aiLeadRecommendationRepository.countByOwnerIdAndUsefulTrue(OWNER_ID)).thenReturn(0L);

		MetricsOverviewResponse response = metricsService.overview(OWNER_ID);

		assertThat(response.totalLeads()).isZero();
		assertThat(response.openLeads()).isZero();
		assertThat(response.closedLeads()).isZero();
		assertThat(response.lostLeads()).isZero();
		assertThat(response.conversionRatePercentage()).isEqualByComparingTo("0.00");
		assertThat(response.averageDaysToClose()).isNull();
		assertThat(response.aiRecommendedLeads()).isZero();
		assertThat(response.aiRecommendedClosedLeads()).isZero();
		assertThat(response.aiRecommendationActioned()).isZero();
		assertThat(response.aiRecommendationUseful()).isZero();
		assertThat(response.aiRecommendationConversionRatePercentage()).isEqualByComparingTo("0.00");
		assertThat(response.aiRecommendationActionRatePercentage()).isEqualByComparingTo("0.00");
		assertThat(response.leadsByStatus()).hasSize(LeadStatus.values().length);
		assertThat(response.leadsByStatus()).extracting(LeadStatusMetricResponse::total)
				.containsOnly(0L);
		assertThat(response.conversionBySource()).hasSize(LeadSource.values().length);
		assertThat(response.conversionBySource()).extracting(LeadSourceConversionResponse::totalLeads)
				.containsOnly(0L);
	}

	@Test
	void overviewShouldCalculateFunnelMetricsForOwner() {
		when(leadRepository.countByOwnerId(OWNER_ID)).thenReturn(5L);
		when(leadRepository.countByOwnerIdAndStatus(OWNER_ID, LeadStatus.CLOSED)).thenReturn(2L);
		when(leadRepository.countByOwnerIdAndStatus(OWNER_ID, LeadStatus.LOST)).thenReturn(1L);
		when(leadRepository.averageDaysToCloseByOwnerId(OWNER_ID)).thenReturn(new BigDecimal("12.345"));
		when(aiLeadRecommendationRepository.countRecommendedLeadsByOwnerId(OWNER_ID)).thenReturn(3L);
		when(aiLeadRecommendationRepository.countRecommendedClosedLeadsByOwnerId(OWNER_ID, LeadStatus.CLOSED))
				.thenReturn(1L);
		when(aiLeadRecommendationRepository.countByOwnerIdAndStatus(OWNER_ID, AiRecommendationStatus.ACTIONED))
				.thenReturn(2L);
		when(aiLeadRecommendationRepository.countByOwnerIdAndUsefulTrue(OWNER_ID)).thenReturn(1L);
		when(leadRepository.countLeadsByStatus(OWNER_ID)).thenReturn(List.of(
				new StatusCountProjection(LeadStatus.NEW, 1L),
				new StatusCountProjection(LeadStatus.CONTACT_MADE, 1L),
				new StatusCountProjection(LeadStatus.CLOSED, 2L),
				new StatusCountProjection(LeadStatus.LOST, 1L)
		));
		when(leadRepository.countConversionBySource(OWNER_ID, LeadStatus.CLOSED)).thenReturn(List.of(
				new SourceConversionProjection(LeadSource.LINKEDIN, 3L, 2L),
				new SourceConversionProjection(LeadSource.WEBSITE, 2L, 0L)
		));

		MetricsOverviewResponse response = metricsService.overview(OWNER_ID);

		assertThat(response.totalLeads()).isEqualTo(5L);
		assertThat(response.openLeads()).isEqualTo(2L);
		assertThat(response.closedLeads()).isEqualTo(2L);
		assertThat(response.lostLeads()).isEqualTo(1L);
		assertThat(response.conversionRatePercentage()).isEqualByComparingTo("40.00");
		assertThat(response.averageDaysToClose()).isEqualByComparingTo("12.35");
		assertThat(response.aiRecommendedLeads()).isEqualTo(3L);
		assertThat(response.aiRecommendedClosedLeads()).isEqualTo(1L);
		assertThat(response.aiRecommendationActioned()).isEqualTo(2L);
		assertThat(response.aiRecommendationUseful()).isEqualTo(1L);
		assertThat(response.aiRecommendationConversionRatePercentage()).isEqualByComparingTo("33.33");
		assertThat(response.aiRecommendationActionRatePercentage()).isEqualByComparingTo("66.67");

		assertStatusTotal(response, LeadStatus.NEW, 1L);
		assertStatusTotal(response, LeadStatus.CONTACT_MADE, 1L);
		assertStatusTotal(response, LeadStatus.PROPOSAL_SENT, 0L);
		assertStatusTotal(response, LeadStatus.NEGOTIATION, 0L);
		assertStatusTotal(response, LeadStatus.CLOSED, 2L);
		assertStatusTotal(response, LeadStatus.LOST, 1L);

		assertSourceConversion(response, LeadSource.REFERRAL, 0L, 0L, "0.00");
		assertSourceConversion(response, LeadSource.LINKEDIN, 3L, 2L, "66.67");
		assertSourceConversion(response, LeadSource.WEBSITE, 2L, 0L, "0.00");
		assertSourceConversion(response, LeadSource.OTHER, 0L, 0L, "0.00");

		verify(leadRepository).countConversionBySource(OWNER_ID, LeadStatus.CLOSED);
	}

	private void assertStatusTotal(MetricsOverviewResponse response, LeadStatus status, Long total) {
		assertThat(response.leadsByStatus())
				.filteredOn(metric -> metric.status() == status)
				.singleElement()
				.extracting(LeadStatusMetricResponse::total)
				.isEqualTo(total);
	}

	private void assertSourceConversion(
			MetricsOverviewResponse response,
			LeadSource source,
			Long totalLeads,
			Long closedLeads,
			String conversionRatePercentage
	) {
		assertThat(response.conversionBySource())
				.filteredOn(metric -> metric.source() == source)
				.singleElement()
				.satisfies(metric -> {
					assertThat(metric.totalLeads()).isEqualTo(totalLeads);
					assertThat(metric.closedLeads()).isEqualTo(closedLeads);
					assertThat(metric.conversionRatePercentage()).isEqualByComparingTo(conversionRatePercentage);
				});
	}

	private record StatusCountProjection(LeadStatus status, Long total) implements LeadStatusCountProjection {

		@Override
		public LeadStatus getStatus() {
			return status;
		}

		@Override
		public Long getTotal() {
			return total;
		}
	}

	private record SourceConversionProjection(
			LeadSource source,
			Long total,
			Long closedLeads
	) implements LeadSourceConversionProjection {

		@Override
		public LeadSource getSource() {
			return source;
		}

		@Override
		public Long getTotal() {
			return total;
		}

		@Override
		public Long getClosedLeads() {
			return closedLeads;
		}
	}
}
