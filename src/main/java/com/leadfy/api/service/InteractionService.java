package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.UpdateInteractionRequest;
import com.leadfy.api.dto.response.InteractionResponse;
import java.util.List;

public interface InteractionService {

	InteractionResponse create(Long ownerId, Long leadId, CreateInteractionRequest request);

	List<InteractionResponse> findAll(Long ownerId, Long leadId);

	InteractionResponse findById(Long ownerId, Long leadId, Long interactionId);

	InteractionResponse update(Long ownerId, Long leadId, Long interactionId, UpdateInteractionRequest request);

	void delete(Long ownerId, Long leadId, Long interactionId);
}
