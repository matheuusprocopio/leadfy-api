package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadRequest;
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.dto.response.LeadResponse;
import com.leadfy.api.dto.response.PageResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface LeadService {

	LeadResponse create(Long ownerId, CreateLeadRequest request);

	PageResponse<LeadResponse> findAll(Long ownerId, Pageable pageable);

	List<LeadResponse> findStale(Long ownerId);

	LeadResponse findById(Long ownerId, Long leadId);

	LeadResponse update(Long ownerId, Long leadId, UpdateLeadRequest request);

	LeadResponse updateStatus(Long ownerId, Long leadId, UpdateLeadStatusRequest request);

	void delete(Long ownerId, Long leadId);
}
