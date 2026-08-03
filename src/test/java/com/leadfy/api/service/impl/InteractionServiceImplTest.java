package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.UpdateInteractionRequest;
import com.leadfy.api.dto.response.InteractionResponse;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

	private static final Long OWNER_ID = 1L;
	private static final Long LEAD_ID = 10L;

	@Mock
	private InteractionRepository interactionRepository;

	@Mock
	private LeadRepository leadRepository;

	@InjectMocks
	private InteractionServiceImpl interactionService;

	@Test
	void createShouldSaveInteractionForOwnedLead() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		LocalDateTime interactionDate = LocalDateTime.now().minusDays(1);
		CreateInteractionRequest request = new CreateInteractionRequest(
				InteractionType.EMAIL,
				" Sent portfolio ",
				interactionDate
		);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.save(any(Interaction.class))).thenAnswer(invocation -> {
			Interaction interaction = invocation.getArgument(0);
			setInteractionPersistenceFields(interaction, 100L);
			return interaction;
		});

		InteractionResponse response = interactionService.create(OWNER_ID, LEAD_ID, request);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.leadId()).isEqualTo(LEAD_ID);
		assertThat(response.type()).isEqualTo(InteractionType.EMAIL);
		assertThat(response.description()).isEqualTo("Sent portfolio");
		assertThat(response.interactionDate()).isEqualTo(interactionDate);

		ArgumentCaptor<Interaction> interactionCaptor = ArgumentCaptor.forClass(Interaction.class);
		verify(interactionRepository).save(interactionCaptor.capture());

		Interaction savedInteraction = interactionCaptor.getValue();
		assertThat(savedInteraction.getLead()).isSameAs(lead);
		assertThat(savedInteraction.getDescription()).isEqualTo("Sent portfolio");
	}

	@Test
	void createShouldRejectLeadFromAnotherOwner() {
		CreateInteractionRequest request = new CreateInteractionRequest(
				InteractionType.CALL,
				"Called lead",
				LocalDateTime.now().minusHours(1)
		);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> interactionService.create(OWNER_ID, LEAD_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Lead not found: 10");

		verify(interactionRepository, never()).save(any(Interaction.class));
	}

	@Test
	void findAllShouldValidateLeadOwnershipAndReturnInteractions() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Interaction firstInteraction = interactionWithId(100L, lead, InteractionType.MEETING);
		Interaction secondInteraction = interactionWithId(101L, lead, InteractionType.WHATSAPP);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(interactionRepository.findByLeadIdAndLeadOwnerIdOrderByInteractionDateDescCreatedAtDesc(LEAD_ID, OWNER_ID))
				.thenReturn(List.of(firstInteraction, secondInteraction));

		List<InteractionResponse> responses = interactionService.findAll(OWNER_ID, LEAD_ID);

		assertThat(responses).hasSize(2);
		assertThat(responses).extracting(InteractionResponse::id).containsExactly(100L, 101L);
	}

	@Test
	void findByIdShouldRejectInteractionFromAnotherOwner() {
		when(interactionRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> interactionService.findById(OWNER_ID, LEAD_ID, 100L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Interaction not found: 100");
	}

	@Test
	void updateShouldChangeInteractionDetails() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Interaction interaction = interactionWithId(100L, lead, InteractionType.CALL);
		LocalDateTime newInteractionDate = LocalDateTime.now().minusHours(2);
		UpdateInteractionRequest request = new UpdateInteractionRequest(
				InteractionType.MEETING,
				" Discovery meeting completed ",
				newInteractionDate
		);

		when(interactionRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(interaction));

		InteractionResponse response = interactionService.update(OWNER_ID, LEAD_ID, 100L, request);

		assertThat(response.type()).isEqualTo(InteractionType.MEETING);
		assertThat(response.description()).isEqualTo("Discovery meeting completed");
		assertThat(response.interactionDate()).isEqualTo(newInteractionDate);
	}

	@Test
	void deleteShouldRemoveOwnedInteraction() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Interaction interaction = interactionWithId(100L, lead, InteractionType.CALL);

		when(interactionRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(interaction));

		interactionService.delete(OWNER_ID, LEAD_ID, 100L);

		verify(interactionRepository).delete(interaction);
	}

	private User userWithId(Long id) {
		User user = new User("Jane Doe", "jane@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Lead leadWithId(Long id, User owner) {
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
		ReflectionTestUtils.setField(lead, "createdAt", LocalDateTime.now());
		ReflectionTestUtils.setField(lead, "updatedAt", LocalDateTime.now());
		return lead;
	}

	private Interaction interactionWithId(Long id, Lead lead, InteractionType type) {
		Interaction interaction = new Interaction(
				type,
				"Initial contact",
				LocalDateTime.now().minusDays(1),
				lead
		);

		setInteractionPersistenceFields(interaction, id);
		return interaction;
	}

	private void setInteractionPersistenceFields(Interaction interaction, Long id) {
		LocalDateTime now = LocalDateTime.now();
		ReflectionTestUtils.setField(interaction, "id", id);
		ReflectionTestUtils.setField(interaction, "createdAt", now);
		ReflectionTestUtils.setField(interaction, "updatedAt", now);
	}
}
