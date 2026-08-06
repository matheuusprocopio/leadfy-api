package com.leadfy.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.dto.response.ProposalResponse;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.ProposalStatus;
import com.leadfy.api.exception.InvalidProposalStatusTransitionException;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import com.leadfy.api.service.ProposalStatusTransitionValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ProposalServiceImplTest {

	private static final Long OWNER_ID = 1L;
	private static final Long LEAD_ID = 10L;

	@Mock
	private ProposalRepository proposalRepository;

	@Mock
	private LeadRepository leadRepository;

	@Mock
	private ProposalStatusTransitionValidator proposalStatusTransitionValidator;

	@InjectMocks
	private ProposalServiceImpl proposalService;

	@Test
	void createShouldSaveSentProposalForOwnedLead() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		LocalDate sentAt = LocalDate.now().minusDays(1);
		CreateProposalRequest request = new CreateProposalRequest(new BigDecimal("2000.00"), sentAt);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> {
			Proposal proposal = invocation.getArgument(0);
			setProposalPersistenceFields(proposal, 100L);
			return proposal;
		});

		ProposalResponse response = proposalService.create(OWNER_ID, LEAD_ID, request);

		assertThat(response.id()).isEqualTo(100L);
		assertThat(response.leadId()).isEqualTo(LEAD_ID);
		assertThat(response.amount()).isEqualByComparingTo("2000.00");
		assertThat(response.status()).isEqualTo(ProposalStatus.SENT);
		assertThat(response.sentAt()).isEqualTo(sentAt);
		assertThat(response.respondedAt()).isNull();

		ArgumentCaptor<Proposal> proposalCaptor = ArgumentCaptor.forClass(Proposal.class);
		verify(proposalRepository).save(proposalCaptor.capture());

		Proposal savedProposal = proposalCaptor.getValue();
		assertThat(savedProposal.getLead()).isSameAs(lead);
		assertThat(savedProposal.getAmount()).isEqualByComparingTo("2000.00");
		assertThat(savedProposal.getStatus()).isEqualTo(ProposalStatus.SENT);
	}

	@Test
	void createShouldRejectLeadFromAnotherOwner() {
		CreateProposalRequest request = new CreateProposalRequest(
				new BigDecimal("1500.00"),
				LocalDate.now().minusDays(1)
		);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> proposalService.create(OWNER_ID, LEAD_ID, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Lead not found: 10");

		verify(proposalRepository, never()).save(any(Proposal.class));
	}

	@Test
	void findAllShouldValidateLeadOwnershipAndReturnProposals() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Proposal firstProposal = proposalWithId(100L, lead);
		Proposal secondProposal = proposalWithId(101L, lead);
		Pageable pageable = PageRequest.of(0, 20);

		when(leadRepository.findByIdAndOwnerId(LEAD_ID, OWNER_ID)).thenReturn(Optional.of(lead));
		when(proposalRepository.findByLeadIdAndLeadOwnerId(LEAD_ID, OWNER_ID, pageable))
				.thenReturn(new PageImpl<>(List.of(firstProposal, secondProposal), pageable, 2));

		PageResponse<ProposalResponse> response = proposalService.findAll(OWNER_ID, LEAD_ID, pageable);

		assertThat(response.content()).hasSize(2);
		assertThat(response.content()).extracting(ProposalResponse::id).containsExactly(100L, 101L);
	}

	@Test
	void findByIdShouldRejectProposalFromAnotherOwner() {
		when(proposalRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> proposalService.findById(OWNER_ID, LEAD_ID, 100L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Proposal not found: 100");
	}

	@Test
	void updateShouldChangeProposalDetails() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Proposal proposal = proposalWithId(100L, lead);
		LocalDate newSentAt = LocalDate.now().minusDays(2);
		UpdateProposalRequest request = new UpdateProposalRequest(new BigDecimal("2500.00"), newSentAt);

		when(proposalRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(proposal));

		ProposalResponse response = proposalService.update(OWNER_ID, LEAD_ID, 100L, request);

		assertThat(response.amount()).isEqualByComparingTo("2500.00");
		assertThat(response.sentAt()).isEqualTo(newSentAt);
	}

	@Test
	void updateStatusShouldValidateAndChangeProposalStatus() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Proposal proposal = proposalWithId(100L, lead);
		LocalDate respondedAt = LocalDate.now();
		UpdateProposalStatusRequest request = new UpdateProposalStatusRequest(ProposalStatus.ACCEPTED, respondedAt);

		when(proposalRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(proposal));

		ProposalResponse response = proposalService.updateStatus(OWNER_ID, LEAD_ID, 100L, request);

		assertThat(response.status()).isEqualTo(ProposalStatus.ACCEPTED);
		assertThat(response.respondedAt()).isEqualTo(respondedAt);
		verify(proposalStatusTransitionValidator).validate(ProposalStatus.SENT, ProposalStatus.ACCEPTED);
	}

	@Test
	void updateStatusShouldNotChangeProposalWhenTransitionIsInvalid() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Proposal proposal = proposalWithIdAndStatus(100L, lead, ProposalStatus.ACCEPTED);
		UpdateProposalStatusRequest request = new UpdateProposalStatusRequest(ProposalStatus.REJECTED, LocalDate.now());

		when(proposalRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(proposal));
		doThrow(new InvalidProposalStatusTransitionException(ProposalStatus.ACCEPTED, ProposalStatus.REJECTED))
				.when(proposalStatusTransitionValidator)
				.validate(ProposalStatus.ACCEPTED, ProposalStatus.REJECTED);

		assertThatThrownBy(() -> proposalService.updateStatus(OWNER_ID, LEAD_ID, 100L, request))
				.isInstanceOf(InvalidProposalStatusTransitionException.class)
				.hasMessage("Invalid proposal status transition from ACCEPTED to REJECTED");

		assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.ACCEPTED);
	}

	@Test
	void deleteShouldRemoveOwnedProposal() {
		Lead lead = leadWithId(LEAD_ID, userWithId(OWNER_ID));
		Proposal proposal = proposalWithId(100L, lead);

		when(proposalRepository.findByIdAndLeadIdAndLeadOwnerId(100L, LEAD_ID, OWNER_ID))
				.thenReturn(Optional.of(proposal));

		proposalService.delete(OWNER_ID, LEAD_ID, 100L);

		verify(proposalRepository).delete(proposal);
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

	private Proposal proposalWithId(Long id, Lead lead) {
		Proposal proposal = new Proposal(
				new BigDecimal("2000.00"),
				LocalDate.now().minusDays(1),
				lead
		);

		setProposalPersistenceFields(proposal, id);
		return proposal;
	}

	private Proposal proposalWithIdAndStatus(Long id, Lead lead, ProposalStatus status) {
		Proposal proposal = proposalWithId(id, lead);
		proposal.updateStatus(status, LocalDate.now());
		return proposal;
	}

	private void setProposalPersistenceFields(Proposal proposal, Long id) {
		LocalDateTime now = LocalDateTime.now();
		ReflectionTestUtils.setField(proposal, "id", id);
		ReflectionTestUtils.setField(proposal, "createdAt", now);
		ReflectionTestUtils.setField(proposal, "updatedAt", now);
	}
}
