package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.request.UpdateAiRecommendationFeedbackRequest;
import com.leadfy.api.dto.response.AiLeadRecommendationResponse;
import com.leadfy.api.entity.AiLeadRecommendation;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.AiLeadRecommendationRepository;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import com.leadfy.api.service.AiLeadInsightContext;
import com.leadfy.api.service.AiLeadInsightContextBuilder;
import com.leadfy.api.service.AiLeadInsightResultNormalizer;
import com.leadfy.api.service.NormalizedAiLeadInsight;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiLeadRecommendationServiceImplTest {

	private static final Long OWNER_ID = 1L;
	private static final Long LEAD_ID = 10L;

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Mock
	private LeadRepository leadRepository;

	@Mock
	private AiLeadRecommendationRepository aiLeadRecommendationRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private ProposalRepository proposalRepository;

	@Mock
	private AiClient aiClient;

	@Captor
	private ArgumentCaptor<AiLeadRecommendation> recommendationCaptor;

	@Test
	void generateForLeadShouldPersistActiveRecommendationAndDeactivatePreviousOnes() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadRecommendationServiceImpl service = serviceWithClient(aiClient);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		when(proposalRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		when(aiClient.generateLeadInsight(any(AiLeadInsightContext.class))).thenReturn(result());
		when(aiLeadRecommendationRepository.save(any(AiLeadRecommendation.class))).thenAnswer(invocation -> {
			AiLeadRecommendation recommendation = invocation.getArgument(0);
			ReflectionTestUtils.setField(recommendation, "id", 99L);
			return recommendation;
		});

		AiLeadRecommendationResponse response = service.generateForLead(OWNER_ID, LEAD_ID);

		assertThat(response.id()).isEqualTo(99L);
		assertThat(response.leadId()).isEqualTo(LEAD_ID);
		assertThat(response.priorityScore()).isEqualTo(91);
		assertThat(response.conversionSignals()).containsExactly("Respondeu rapido");
		assertThat(response.riskSignals()).containsExactly("Sem retorno ha 8 dias");
		assertThat(response.status()).isEqualTo(AiRecommendationStatus.PENDING);
		assertThat(response.useful()).isNull();
		assertThat(response.active()).isTrue();
		assertThat(response.generatedAt()).isNotNull();

		verify(aiLeadRecommendationRepository).deactivateActiveRecommendationsForLead(LEAD_ID);
		verify(aiLeadRecommendationRepository).save(recommendationCaptor.capture());
		assertThat(recommendationCaptor.getValue().getSuggestedMessage())
				.isEqualTo("Oi, posso te ajudar com a proposta?");
	}

	@Test
	void generateForLeadShouldRejectLeadFromAnotherOwner() {
		AiLeadRecommendationServiceImpl service = serviceWithClient(aiClient);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generateForLead(OWNER_ID, LEAD_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Lead not found: 10");

		verify(aiClient, never()).generateLeadInsight(any(AiLeadInsightContext.class));
	}

	@Test
	void updateFeedbackShouldPersistHumanReview() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadRecommendation recommendation = recommendationFor(lead);
		AiLeadRecommendationServiceImpl service = serviceWithClient(aiClient);

		when(aiLeadRecommendationRepository.findByIdAndOwnerId(99L, OWNER_ID)).thenReturn(Optional.of(recommendation));

		AiLeadRecommendationResponse response = service.updateFeedback(
				OWNER_ID,
				99L,
				new UpdateAiRecommendationFeedbackRequest(AiRecommendationStatus.ACTIONED, true)
		);

		assertThat(response.status()).isEqualTo(AiRecommendationStatus.ACTIONED);
		assertThat(response.useful()).isTrue();
		assertThat(response.reviewedAt()).isNotNull();
	}

	@Test
	void refreshOpenLeadRecommendationsShouldStopWhenAiIsUnavailable() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadRecommendationServiceImpl service = serviceWithoutClient();

		when(leadRepository.findLeadsEligibleForAiRecommendations(any(Instant.class), any(), any(Pageable.class)))
				.thenReturn(List.of(lead));

		int refreshed = service.refreshOpenLeadRecommendations(10);

		assertThat(refreshed).isZero();
		verify(aiLeadRecommendationRepository, never()).save(any(AiLeadRecommendation.class));
	}

	@Test
	void generateForLeadShouldFailWhenAiClientIsUnavailable() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadRecommendationServiceImpl service = serviceWithoutClient();

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));

		assertThatThrownBy(() -> service.generateForLead(OWNER_ID, LEAD_ID))
				.isInstanceOf(AiInsightsUnavailableException.class);
	}

	private AiLeadRecommendationServiceImpl serviceWithClient(AiClient client) {
		return new AiLeadRecommendationServiceImpl(
				leadRepository,
				aiLeadRecommendationRepository,
				new AiLeadInsightContextBuilder(leadRepository, interactionRepository, proposalRepository),
				new AiLeadInsightResultNormalizer(),
				objectMapper,
				List.of(client)
		);
	}

	private AiLeadRecommendationServiceImpl serviceWithoutClient() {
		return new AiLeadRecommendationServiceImpl(
				leadRepository,
				aiLeadRecommendationRepository,
				new AiLeadInsightContextBuilder(leadRepository, interactionRepository, proposalRepository),
				new AiLeadInsightResultNormalizer(),
				objectMapper,
				List.of()
		);
	}

	private AiLeadInsightResult result() {
		return new AiLeadInsightResult(
				91,
				"Lead quente",
				List.of("Respondeu rapido"),
				List.of("Sem retorno ha 8 dias"),
				"Enviar follow-up objetivo",
				"Oi, posso te ajudar com a proposta?",
				"HIGH"
		);
	}

	private User userWithId(Long id) {
		User user = new User("Maria Freelancer", "maria@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Lead leadWithId(User owner) {
		Lead lead = new Lead(
				"Maria Cliente",
				"Acme Inc",
				"cliente@acme.com",
				null,
				LeadSource.WEBSITE,
				"Precisa automatizar a captacao",
				owner
		);
		ReflectionTestUtils.setField(lead, "id", LEAD_ID);
		ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.of(2026, 8, 1, 10, 0));
		ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.of(2026, 8, 20, 10, 0));
		return lead;
	}

	private AiLeadRecommendation recommendationFor(Lead lead) {
		AiLeadRecommendation recommendation = new AiLeadRecommendation(
				lead,
				"[\"Sinal\"]",
				"[\"Risco\"]",
				new NormalizedAiLeadInsight(
						80,
						"Resumo",
						List.of("Sinal"),
						List.of("Risco"),
						"Agir",
						"Mensagem",
						"MEDIUM"
				),
				Instant.now()
		);
		ReflectionTestUtils.setField(recommendation, "id", 99L);
		return recommendation;
	}
}
