package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.dto.response.LeadResponse;
import java.util.List;

public interface LeadService {

	LeadResponse create(Long ownerId, CreateLeadRequest request);

	List<LeadResponse> findAll(Long ownerId);

	List<LeadResponse> findStale(Long ownerId);

	LeadResponse findById(Long ownerId, Long leadId);

	LeadResponse update(Long ownerId, Long leadId, UpdateLeadRequest request);

	LeadResponse updateStatus(Long ownerId, Long leadId, UpdateLeadStatusRequest request);

	void delete(Long ownerId, Long leadId);
}
