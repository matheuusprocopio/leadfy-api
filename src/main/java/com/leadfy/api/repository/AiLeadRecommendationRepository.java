package com.leadfy.api.repository;

import com.leadfy.api.entity.AiLeadRecommendation;
import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiLeadRecommendationRepository extends JpaRepository<AiLeadRecommendation, Long> {

	Page<AiLeadRecommendation> findByOwnerIdAndActiveTrue(Long ownerId, Pageable pageable);

	Optional<AiLeadRecommendation> findByIdAndOwnerId(Long id, Long ownerId);

	@Modifying
	@Query("""
			UPDATE AiLeadRecommendation recommendation
			SET recommendation.active = false
			WHERE recommendation.lead.id = :leadId
			  AND recommendation.active = true
			""")
	void deactivateActiveRecommendationsForLead(@Param("leadId") Long leadId);

	@Query("""
			SELECT COUNT(DISTINCT recommendation.lead.id)
			FROM AiLeadRecommendation recommendation
			WHERE recommendation.owner.id = :ownerId
			""")
	Long countRecommendedLeadsByOwnerId(@Param("ownerId") Long ownerId);

	@Query("""
			SELECT COUNT(DISTINCT recommendation.lead.id)
			FROM AiLeadRecommendation recommendation
			WHERE recommendation.owner.id = :ownerId
			  AND recommendation.lead.status = :closedStatus
			""")
	Long countRecommendedClosedLeadsByOwnerId(
			@Param("ownerId") Long ownerId,
			@Param("closedStatus") LeadStatus closedStatus
	);

	Long countByOwnerIdAndStatus(Long ownerId, AiRecommendationStatus status);

	Long countByOwnerIdAndUsefulTrue(Long ownerId);
}
