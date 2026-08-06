package com.leadfy.api.service.impl;

import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.dto.response.ProposalResponse;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.ProposalRepository;
import com.leadfy.api.service.ProposalService;
import com.leadfy.api.service.ProposalStatusTransitionValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProposalServiceImpl implements ProposalService {

	private final ProposalRepository proposalRepository;
	private final LeadRepository leadRepository;
	private final ProposalStatusTransitionValidator proposalStatusTransitionValidator;

	public ProposalServiceImpl(
			ProposalRepository proposalRepository,
			LeadRepository leadRepository,
			ProposalStatusTransitionValidator proposalStatusTransitionValidator
	) {
		this.proposalRepository = proposalRepository;
		this.leadRepository = leadRepository;
		this.proposalStatusTransitionValidator = proposalStatusTransitionValidator;
	}

	@Override
	@Transactional
	public ProposalResponse create(Long ownerId, Long leadId, CreateProposalRequest request) {
		Lead lead = findLeadByIdAndOwnerId(leadId, ownerId);

		Proposal proposal = new Proposal(
				request.amount(),
				request.sentAt(),
				lead
		);

		return toResponse(proposalRepository.save(proposal));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProposalResponse> findAll(Long ownerId, Long leadId, Pageable pageable) {
		findLeadByIdAndOwnerId(leadId, ownerId);

		return PageResponse.from(
				proposalRepository.findByLeadIdAndLeadOwnerId(leadId, ownerId, pageable).map(this::toResponse)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public ProposalResponse findById(Long ownerId, Long leadId, Long proposalId) {
		return toResponse(findProposalByIdLeadIdAndOwnerId(proposalId, leadId, ownerId));
	}

	@Override
	@Transactional
	public ProposalResponse update(Long ownerId, Long leadId, Long proposalId, UpdateProposalRequest request) {
		Proposal proposal = findProposalByIdLeadIdAndOwnerId(proposalId, leadId, ownerId);

		proposal.updateDetails(
				request.amount(),
				request.sentAt()
		);

		return toResponse(proposal);
	}

	@Override
	@Transactional
	public ProposalResponse updateStatus(
			Long ownerId,
			Long leadId,
			Long proposalId,
			UpdateProposalStatusRequest request
	) {
		Proposal proposal = findProposalByIdLeadIdAndOwnerId(proposalId, leadId, ownerId);
		proposalStatusTransitionValidator.validate(proposal.getStatus(), request.status());
		proposal.updateStatus(request.status(), request.respondedAt());
		return toResponse(proposal);
	}

	@Override
	@Transactional
	public void delete(Long ownerId, Long leadId, Long proposalId) {
		Proposal proposal = findProposalByIdLeadIdAndOwnerId(proposalId, leadId, ownerId);
		proposalRepository.delete(proposal);
	}

	private Lead findLeadByIdAndOwnerId(Long leadId, Long ownerId) {
		return leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));
	}

	private Proposal findProposalByIdLeadIdAndOwnerId(Long proposalId, Long leadId, Long ownerId) {
		return proposalRepository.findByIdAndLeadIdAndLeadOwnerId(proposalId, leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId));
	}

	private ProposalResponse toResponse(Proposal proposal) {
		return new ProposalResponse(
				proposal.getId(),
				proposal.getLead().getId(),
				proposal.getAmount(),
				proposal.getStatus(),
				proposal.getSentAt(),
				proposal.getRespondedAt(),
				proposal.getCreatedAt(),
				proposal.getUpdatedAt()
		);
	}
}
