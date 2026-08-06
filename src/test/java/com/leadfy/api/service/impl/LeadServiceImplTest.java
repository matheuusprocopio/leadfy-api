package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.dto.response.LeadResponse;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.InvalidLeadStatusTransitionException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.UserRepository;
import com.leadfy.api.service.LeadStatusTransitionValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

	private static final Long OWNER_ID = 1L;

	@Mock
	private LeadRepository leadRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private LeadStatusTransitionValidator leadStatusTransitionValidator;

	@InjectMocks
	private LeadServiceImpl leadService;

	@Test
	void createShouldSaveLeadForAuthenticatedOwner() {
		User owner = userWithId(OWNER_ID);
		CreateLeadRequest request = new CreateLeadRequest(
				" Acme Contact ",
				" Acme Inc ",
				"CONTACT@ACME.COM",
				null,
				LeadSource.LINKEDIN,
				" Interested in automation "
		);

		when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
		when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> {
			Lead lead = invocation.getArgument(0);
			setLeadPersistenceFields(lead, 10L);
			return lead;
		});

		LeadResponse response = leadService.create(OWNER_ID, request);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.name()).isEqualTo("Acme Contact");
		assertThat(response.company()).isEqualTo("Acme Inc");
		assertThat(response.email()).isEqualTo("contact@acme.com");
		assertThat(response.status()).isEqualTo(LeadStatus.NEW);
		assertThat(response.source()).isEqualTo(LeadSource.LINKEDIN);

		ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
		verify(leadRepository).save(leadCaptor.capture());

		Lead savedLead = leadCaptor.getValue();
		assertThat(savedLead.getOwner()).isSameAs(owner);
		assertThat(savedLead.getNotes()).isEqualTo("Interested in automation");
	}

	@Test
	void findAllShouldReturnOnlyLeadsFromOwner() {
		Lead firstLead = leadWithId(10L, userWithId(OWNER_ID));
		Lead secondLead = leadWithId(11L, userWithId(OWNER_ID));
		Pageable pageable = PageRequest.of(0, 20);

		when(leadRepository.findByOwnerId(OWNER_ID, pageable))
				.thenReturn(new PageImpl<>(List.of(firstLead, secondLead), pageable, 2));

		PageResponse<LeadResponse> response = leadService.findAll(OWNER_ID, pageable);

		assertThat(response.content()).hasSize(2);
		assertThat(response.totalElements()).isEqualTo(2);
		assertThat(response.content()).extracting(LeadResponse::id).containsExactly(10L, 11L);
	}

	@Test
	void findStaleShouldReturnOnlyLeadsFlaggedAsStale() {
		Lead staleLead = leadWithId(10L, userWithId(OWNER_ID));
		staleLead.markAsStale();

		when(leadRepository.findByOwnerIdAndStaleLeadTrueOrderByCreatedAtDesc(OWNER_ID))
				.thenReturn(List.of(staleLead));

		List<LeadResponse> responses = leadService.findStale(OWNER_ID);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).staleLead()).isTrue();
	}

	@Test
	void findByIdShouldRejectLeadFromAnotherOwner() {
		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> leadService.findById(OWNER_ID, 10L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Lead not found: 10");
	}

	@Test
	void updateShouldChangeLeadDetails() {
		Lead lead = leadWithId(10L, userWithId(OWNER_ID));
		UpdateLeadRequest request = new UpdateLeadRequest(
				" Updated Contact ",
				null,
				null,
				" +55 11 99999-9999 ",
				LeadSource.REFERRAL,
				" Updated note "
		);

		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.of(lead));

		LeadResponse response = leadService.update(OWNER_ID, 10L, request);

		assertThat(response.name()).isEqualTo("Updated Contact");
		assertThat(response.company()).isNull();
		assertThat(response.email()).isNull();
		assertThat(response.phone()).isEqualTo("+55 11 99999-9999");
		assertThat(response.source()).isEqualTo(LeadSource.REFERRAL);
		assertThat(response.notes()).isEqualTo("Updated note");
	}

	@Test
	void updateStatusShouldValidateAndChangeLeadStatus() {
		Lead lead = leadWithIdAndStatus(10L, userWithId(OWNER_ID), LeadStatus.NEGOTIATION);
		UpdateLeadStatusRequest request = new UpdateLeadStatusRequest(LeadStatus.CLOSED);

		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.of(lead));

		LeadResponse response = leadService.updateStatus(OWNER_ID, 10L, request);

		assertThat(response.status()).isEqualTo(LeadStatus.CLOSED);
		assertThat(response.closedAt()).isNotNull();
		verify(leadStatusTransitionValidator).validate(LeadStatus.NEGOTIATION, LeadStatus.CLOSED);
	}

	@Test
	void updateStatusShouldClearStaleFlagWhenLeadIsClosedOrLost() {
		Lead lead = leadWithIdAndStatus(10L, userWithId(OWNER_ID), LeadStatus.NEGOTIATION);
		lead.markAsStale();
		UpdateLeadStatusRequest request = new UpdateLeadStatusRequest(LeadStatus.CLOSED);

		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.of(lead));

		LeadResponse response = leadService.updateStatus(OWNER_ID, 10L, request);

		assertThat(response.staleLead()).isFalse();
	}

	@Test
	void updateStatusShouldNotChangeLeadWhenTransitionIsInvalid() {
		Lead lead = leadWithId(10L, userWithId(OWNER_ID));
		UpdateLeadStatusRequest request = new UpdateLeadStatusRequest(LeadStatus.CLOSED);

		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.of(lead));
		doThrow(new InvalidLeadStatusTransitionException(LeadStatus.NEW, LeadStatus.CLOSED))
				.when(leadStatusTransitionValidator)
				.validate(LeadStatus.NEW, LeadStatus.CLOSED);

		assertThatThrownBy(() -> leadService.updateStatus(OWNER_ID, 10L, request))
				.isInstanceOf(InvalidLeadStatusTransitionException.class)
				.hasMessage("Invalid lead status transition from NEW to CLOSED");

		assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
		assertThat(lead.getClosedAt()).isNull();
	}

	@Test
	void deleteShouldRemoveOwnedLead() {
		Lead lead = leadWithId(10L, userWithId(OWNER_ID));

		when(leadRepository.findByIdAndOwnerId(10L, OWNER_ID)).thenReturn(Optional.of(lead));

		leadService.delete(OWNER_ID, 10L);

		verify(leadRepository).delete(lead);
	}

	private User userWithId(Long id) {
		User user = new User("Jane Doe", "jane@example.com", "encoded-password");
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private Lead leadWithId(Long id, User owner) {
		return leadWithIdAndStatus(id, owner, LeadStatus.NEW);
	}

	private Lead leadWithIdAndStatus(Long id, User owner, LeadStatus status) {
		Lead lead = new Lead(
				"Acme Contact",
				"Acme Inc",
				"contact@acme.com",
				null,
				LeadSource.WEBSITE,
				null,
				owner
		);

		setLeadPersistenceFields(lead, id);
		lead.updateStatus(status);
		return lead;
	}

	private void setLeadPersistenceFields(Lead lead, Long id) {
		LocalDateTime now = LocalDateTime.now();
		ReflectionTestUtils.setField(lead, "id", id);
		ReflectionTestUtils.setField(lead, "createdAt", now);
		ReflectionTestUtils.setField(lead, "updatedAt", now);
	}
}
