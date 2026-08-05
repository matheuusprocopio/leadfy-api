package com.leadfy.api.service;

import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.dto.response.ProposalResponse;
import java.util.List;

public interface ProposalService {

	ProposalResponse create(Long ownerId, Long leadId, CreateProposalRequest request);

	List<ProposalResponse> findAll(Long ownerId, Long leadId);

	ProposalResponse findById(Long ownerId, Long leadId, Long proposalId);

	ProposalResponse update(Long ownerId, Long leadId, Long proposalId, UpdateProposalRequest request);

	ProposalResponse updateStatus(Long ownerId, Long leadId, Long proposalId, UpdateProposalStatusRequest request);

	void delete(Long ownerId, Long leadId, Long proposalId);
}
