package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.response.AiLeadInsightResponse;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.AiResponseParsingException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import com.leadfy.api.service.AiLeadInsightContext;
import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiLeadInsightServiceImplTest {

	private static final Long OWNER_ID = 1L;
	private static final Long LEAD_ID = 10L;

	@Mock
	private LeadRepository leadRepository;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private ProposalRepository proposalRepository;

	@Mock
	private AiClient aiClient;

	@Captor
	private ArgumentCaptor<AiLeadInsightContext> contextCaptor;

	@Test
	void generateShouldBuildContextAndReturnValidatedInsight() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		lead.updateStatus(LeadStatus.NEGOTIATION);
		lead.markAsStale();
		Interaction interaction = interactionFor(lead);
		Proposal proposal = proposalFor(lead);
		AiLeadInsightServiceImpl service = serviceWithClient(aiClient);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(interaction)));
		when(proposalRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(proposal)));
		when(aiClient.generateLeadInsight(any(AiLeadInsightContext.class))).thenReturn(new AiLeadInsightResult(
				82,
				" Lead com sinais positivos ",
				List.of("Proposta enviada"),
				List.of("Sem retorno recente"),
				"Enviar follow-up consultivo",
				"Oi, posso ajudar com alguma duvida sobre a proposta?",
				"medium"
		));

		AiLeadInsightResponse response = service.generate(OWNER_ID, LEAD_ID);

		assertThat(response.priorityScore()).isEqualTo(82);
		assertThat(response.summary()).isEqualTo("Lead com sinais positivos");
		assertThat(response.conversionSignals()).containsExactly("Proposta enviada");
		assertThat(response.riskSignals()).containsExactly("Sem retorno recente");
		assertThat(response.nextBestAction()).isEqualTo("Enviar follow-up consultivo");
		assertThat(response.suggestedMessage()).isEqualTo("Oi, posso ajudar com alguma duvida sobre a proposta?");
		assertThat(response.confidence()).isEqualTo("MEDIUM");
		assertThat(response.generatedAt()).isNotNull();

		verify(aiClient).generateLeadInsight(contextCaptor.capture());
		AiLeadInsightContext context = contextCaptor.getValue();
		assertThat(context.leadName()).isEqualTo("Maria Cliente");
		assertThat(context.company()).isEqualTo("Acme Inc");
		assertThat(context.source()).isEqualTo(LeadSource.WEBSITE);
		assertThat(context.status()).isEqualTo(LeadStatus.NEGOTIATION);
		assertThat(context.staleLead()).isTrue();
		assertThat(context.notes()).isEqualTo("Precisa de automacao comercial");
		assertThat(context.recentInteractions()).hasSize(1);
		assertThat(context.recentInteractions().getFirst().description()).isEqualTo("Retornou interesse por WhatsApp");
		assertThat(context.recentProposals()).hasSize(1);
		assertThat(context.recentProposals().getFirst().amount()).isEqualByComparingTo("2500.00");
	}

	@Test
	void generateShouldUseOnlyOwnedLead() {
		AiLeadInsightServiceImpl service = serviceWithClient(aiClient);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(OWNER_ID, LEAD_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Lead not found: 10");

		verify(aiClient, never()).generateLeadInsight(any(AiLeadInsightContext.class));
	}

	@Test
	void generateShouldFailWhenAiClientIsUnavailable() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadInsightServiceImpl service = serviceWithoutClient();

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		when(proposalRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));

		assertThatThrownBy(() -> service.generate(OWNER_ID, LEAD_ID))
				.isInstanceOf(AiInsightsUnavailableException.class);
	}

	@Test
	void generateShouldRejectInvalidAiPriorityScore() {
		User owner = userWithId(OWNER_ID);
		Lead lead = leadWithId(owner);
		AiLeadInsightServiceImpl service = serviceWithClient(aiClient);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		when(proposalRepository.findByLeadIdAndLeadOwnerId(eq(LEAD_ID), eq(OWNER_ID), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of()));
		when(aiClient.generateLeadInsight(any(AiLeadInsightContext.class))).thenReturn(new AiLeadInsightResult(
				101,
				"Resumo",
				List.of(),
				List.of(),
				"Proxima acao",
				"Mensagem sugerida",
				"HIGH"
		));

		assertThatThrownBy(() -> service.generate(OWNER_ID, LEAD_ID))
				.isInstanceOf(AiResponseParsingException.class);
	}

	private AiLeadInsightServiceImpl serviceWithClient(AiClient client) {
		return new AiLeadInsightServiceImpl(
				leadRepository,
				interactionRepository,
				proposalRepository,
				List.of(client)
		);
	}

	private AiLeadInsightServiceImpl serviceWithoutClient() {
		return new AiLeadInsightServiceImpl(
				leadRepository,
				interactionRepository,
				proposalRepository,
				List.of()
		);
	}

	private User userWithId(Long id) {
		User user = new User("Maria Freelancer", "maria@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Lead leadWithId(User owner) {
		Lead lead = new Lead(
				" Maria Cliente ",
				" Acme Inc ",
				"cliente@acme.com",
				"+55 11 99999-9999",
				LeadSource.WEBSITE,
				" Precisa de automacao comercial ",
				owner
		);
		LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
		ReflectionTestUtils.setField(lead, "id", LEAD_ID);
		ReflectionTestUtils.setField(lead, "createdAt", now.minusDays(10));
		ReflectionTestUtils.setField(lead, "updatedAt", now.minusDays(1));
		return lead;
	}

	private Interaction interactionFor(Lead lead) {
		Interaction interaction = new Interaction(
				InteractionType.WHATSAPP,
				" Retornou interesse por WhatsApp ",
				LocalDateTime.of(2026, 8, 23, 15, 30),
				lead
		);
		ReflectionTestUtils.setField(interaction, "id", 100L);
		return interaction;
	}

	private Proposal proposalFor(Lead lead) {
		Proposal proposal = new Proposal(
				new BigDecimal("2500.00"),
				LocalDate.of(2026, 8, 20),
				lead
		);
		ReflectionTestUtils.setField(proposal, "id", 200L);
		return proposal;
	}
}
