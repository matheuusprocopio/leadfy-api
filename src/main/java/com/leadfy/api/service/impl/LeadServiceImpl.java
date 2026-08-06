package com.leadfy.api.service.impl;

import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.dto.response.LeadResponse;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.exception.ResourceNotFoundException;
import com.leadfy.api.repository.LeadRepository;
import com.leadfy.api.repository.UserRepository;
import com.leadfy.api.service.LeadService;
import com.leadfy.api.service.LeadStatusTransitionValidator;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadServiceImpl implements LeadService {

	private final LeadRepository leadRepository;
	private final UserRepository userRepository;
	private final LeadStatusTransitionValidator leadStatusTransitionValidator;

	public LeadServiceImpl(
			LeadRepository leadRepository,
			UserRepository userRepository,
			LeadStatusTransitionValidator leadStatusTransitionValidator
	) {
		this.leadRepository = leadRepository;
		this.userRepository = userRepository;
		this.leadStatusTransitionValidator = leadStatusTransitionValidator;
	}

	@Override
	@Transactional
	public LeadResponse create(Long ownerId, CreateLeadRequest request) {
		User owner = userRepository.findById(ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

		Lead lead = new Lead(
				normalizeRequiredText(request.name()),
				normalizeOptionalText(request.company()),
				normalizeEmail(request.email()),
				normalizeOptionalText(request.phone()),
				request.source(),
				normalizeOptionalText(request.notes()),
				owner
		);

		return toResponse(leadRepository.save(lead));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<LeadResponse> findAll(Long ownerId, Pageable pageable) {
		return PageResponse.from(leadRepository.findByOwnerId(ownerId, pageable).map(this::toResponse));
	}

	@Override
	@Transactional(readOnly = true)
	public List<LeadResponse> findStale(Long ownerId) {
		return leadRepository.findByOwnerIdAndStaleLeadTrueOrderByCreatedAtDesc(ownerId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public LeadResponse findById(Long ownerId, Long leadId) {
		return toResponse(findLeadByIdAndOwnerId(leadId, ownerId));
	}

	@Override
	@Transactional
	public LeadResponse update(Long ownerId, Long leadId, UpdateLeadRequest request) {
		Lead lead = findLeadByIdAndOwnerId(leadId, ownerId);

		lead.updateDetails(
				normalizeRequiredText(request.name()),
				normalizeOptionalText(request.company()),
				normalizeEmail(request.email()),
				normalizeOptionalText(request.phone()),
				request.source(),
				normalizeOptionalText(request.notes())
		);

		return toResponse(lead);
	}

	@Override
	@Transactional
	public LeadResponse updateStatus(Long ownerId, Long leadId, UpdateLeadStatusRequest request) {
		Lead lead = findLeadByIdAndOwnerId(leadId, ownerId);
		leadStatusTransitionValidator.validate(lead.getStatus(), request.status());
		lead.updateStatus(request.status());
		return toResponse(lead);
	}

	@Override
	@Transactional
	public void delete(Long ownerId, Long leadId) {
		Lead lead = findLeadByIdAndOwnerId(leadId, ownerId);
		leadRepository.delete(lead);
	}

	private Lead findLeadByIdAndOwnerId(Long leadId, Long ownerId) {
		return leadRepository.findByIdAndOwnerId(leadId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));
	}

	private LeadResponse toResponse(Lead lead) {
		return new LeadResponse(
				lead.getId(),
				lead.getName(),
				lead.getCompany(),
				lead.getEmail(),
				lead.getPhone(),
				lead.getSource(),
				lead.getStatus(),
				lead.getNotes(),
				lead.getCreatedAt(),
				lead.getUpdatedAt(),
				lead.getClosedAt(),
				lead.isStaleLead()
		);
	}

	private String normalizeRequiredText(String value) {
		return value.trim();
	}

	private String normalizeOptionalText(String value) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		return value.trim();
	}

	private String normalizeEmail(String value) {
		String normalized = normalizeOptionalText(value);

		if (normalized == null) {
			return null;
		}

		return normalized.toLowerCase(Locale.ROOT);
	}
}
