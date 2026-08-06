package com.leadfy.api.repository;

import com.leadfy.api.entity.Proposal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

	Page<Proposal> findByLeadIdAndLeadOwnerId(Long leadId, Long ownerId, Pageable pageable);

	Optional<Proposal> findByIdAndLeadIdAndLeadOwnerId(Long id, Long leadId, Long ownerId);
}
