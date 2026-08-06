package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.UpdateInteractionRequest;
import com.leadfy.api.dto.response.InteractionResponse;
import com.leadfy.api.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface InteractionService {

	InteractionResponse create(Long ownerId, Long leadId, CreateInteractionRequest request);

	PageResponse<InteractionResponse> findAll(Long ownerId, Long leadId, Pageable pageable);

	InteractionResponse findById(Long ownerId, Long leadId, Long interactionId);

	InteractionResponse update(Long ownerId, Long leadId, Long interactionId, UpdateInteractionRequest request);

	void delete(Long ownerId, Long leadId, Long interactionId);
}
