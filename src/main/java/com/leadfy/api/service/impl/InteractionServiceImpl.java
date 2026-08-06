package com.leadfy.api.service.impl;

import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.UpdateInteractionRequest;
import com.leadfy.api.dto.response.InteractionResponse;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.InteractionRepository;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.service.InteractionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionServiceImpl implements InteractionService {

	private final InteractionRepository interactionRepository;
	private final LeadRepository leadRepository;

	public InteractionServiceImpl(InteractionRepository interactionRepository, LeadRepository leadRepository) {
		this.interactionRepository = interactionRepository;
		this.leadRepository = leadRepository;
	}

	@Override
	@Transactional
	public InteractionResponse create(Long ownerId, Long leadId, CreateInteractionRequest request) {
		Lead lead = findLeadByIdAndOwnerId(leadId, ownerId);
		lead.clearStaleFlag();

		Interaction interaction = new Interaction(
				request.type(),
				normalizeDescription(request.description()),
				request.interactionDate(),
				lead
		);

		return toResponse(interactionRepository.save(interaction));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<InteractionResponse> findAll(Long ownerId, Long leadId, Pageable pageable) {
		findLeadByIdAndOwnerId(leadId, ownerId);

		return PageResponse.from(
				interactionRepository.findByLeadIdAndLeadOwnerId(leadId, ownerId, pageable).map(this::toResponse)
		);
	}

	@Override
	@Transactional(readOnly = true)
	public InteractionResponse findById(Long ownerId, Long leadId, Long interactionId) {
		return toResponse(findInteractionByIdLeadIdAndOwnerId(interactionId, leadId, ownerId));
	}

	@Override
	@Transactional
	public InteractionResponse update(Long ownerId, Long leadId, Long interactionId, UpdateInteractionRequest request) {
		Interaction interaction = findInteractionByIdLeadIdAndOwnerId(interactionId, leadId, ownerId);

		interaction.updateDetails(
				request.type(),
				normalizeDescription(request.description()),
				request.interactionDate()
		);

		return toResponse(interaction);
	}

	@Override
	@Transactional
	public void delete(Long ownerId, Long leadId, Long interactionId) {
		Interaction interaction = findInteractionByIdLeadIdAndOwnerId(interactionId, leadId, ownerId);
		interactionRepository.delete(interaction);
	}

	private Lead findLeadByIdAndOwnerId(Long leadId, Long ownerId) {
		return leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));
	}

	private Interaction findInteractionByIdLeadIdAndOwnerId(Long interactionId, Long leadId, Long ownerId) {
		return interactionRepository.findByIdAndLeadIdAndLeadOwnerId(interactionId, leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Interaction", interactionId));
	}

	private InteractionResponse toResponse(Interaction interaction) {
		return new InteractionResponse(
				interaction.getId(),
				interaction.getLead().getId(),
				interaction.getType(),
				interaction.getDescription(),
				interaction.getInteractionDate(),
				interaction.getCreatedAt(),
				interaction.getUpdatedAt()
		);
	}

	private String normalizeDescription(String description) {
		return description.trim();
	}
}
