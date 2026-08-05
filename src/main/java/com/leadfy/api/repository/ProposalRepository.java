package com.leadfy.api.repository;

import com.leadfy.api.entity.Proposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {

	List<Proposal> findByLeadIdAndLeadOwnerIdOrderBySentAtDescCreatedAtDesc(Long leadId, Long ownerId);

	Optional<Proposal> findByIdAndLeadIdAndLeadOwnerId(Long id, Long leadId, Long ownerId);
}
