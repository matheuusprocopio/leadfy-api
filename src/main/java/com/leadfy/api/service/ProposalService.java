package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.dto.response.PageResponse;
import com.leadfy.api.dto.response.ProposalResponse;
import org.springframework.data.domain.Pageable;

public interface ProposalService {

	ProposalResponse create(Long ownerId, Long leadId, CreateProposalRequest request);

	PageResponse<ProposalResponse> findAll(Long ownerId, Long leadId, Pageable pageable);

	ProposalResponse findById(Long ownerId, Long leadId, Long proposalId);

	ProposalResponse update(Long ownerId, Long leadId, Long proposalId, UpdateProposalRequest request);

	ProposalResponse updateStatus(Long ownerId, Long leadId, Long proposalId, UpdateProposalStatusRequest request);

	void delete(Long ownerId, Long leadId, Long proposalId);
}
