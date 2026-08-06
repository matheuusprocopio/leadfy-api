package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.repository.LeadRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaleLeadServiceImplTest {

	private static final int THRESHOLD_DAYS = 7;

	@Mock
	private LeadRepository leadRepository;

	@Test
	void flagStaleLeadsShouldMarkEligibleLeadsAsStale() {
		StaleLeadServiceImpl staleLeadService = new StaleLeadServiceImpl(leadRepository, THRESHOLD_DAYS);

		Lead firstLead = leadWithId(10L);
		Lead secondLead = leadWithId(11L);

		when(leadRepository.findLeadsEligibleForStaleFlag(any(LocalDateTime.class), anyCollection()))
				.thenReturn(List.of(firstLead, secondLead));

		int flaggedCount = staleLeadService.flagStaleLeads();

		assertThat(flaggedCount).isEqualTo(2);
		assertThat(firstLead.isStaleLead()).isTrue();
		assertThat(secondLead.isStaleLead()).isTrue();
	}

	@Test
	void flagStaleLeadsShouldReturnZeroWhenNoLeadIsEligible() {
		StaleLeadServiceImpl staleLeadService = new StaleLeadServiceImpl(leadRepository, THRESHOLD_DAYS);

		when(leadRepository.findLeadsEligibleForStaleFlag(any(LocalDateTime.class), anyCollection()))
				.thenReturn(List.of());

		int flaggedCount = staleLeadService.flagStaleLeads();

		assertThat(flaggedCount).isZero();
	}

	private Lead leadWithId(Long id) {
		User owner = new User("Jane Doe", "jane@example.com", "encoded-password");
		ReflectionTestUtils.setField(owner, "id", 1L);

		Lead lead = new Lead(
				"Acme Contact",
				"Acme Inc",
				"contact@acme.com",
				null,
				LeadSource.WEBSITE,
				null,
				owner
		);

		ReflectionTestUtils.setField(lead, "id", id);
		ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.now().minusDays(10));
		ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.now().minusDays(10));
		return lead;
	}
}
