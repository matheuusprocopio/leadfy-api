package com.leadfy.api.repository;

import com.leadfy.api.entity.Lead;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.repository.projection.LeadSourceConversionProjection;
import com.leadfy.api.repository.projection.LeadStatusCountProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long> {

	List<Lead> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

	Optional<Lead> findByIdAndOwnerId(Long id, Long ownerId);

	List<Lead> findByOwnerIdAndStaleLeadTrueOrderByCreatedAtDesc(Long ownerId);

	Long countByOwnerId(Long ownerId);

	Long countByOwnerIdAndStatus(Long ownerId, LeadStatus status);

	@Query("""
			SELECT lead.status AS status, COUNT(lead) AS total
			FROM Lead lead
			WHERE lead.owner.id = :ownerId
			GROUP BY lead.status
			""")
	List<LeadStatusCountProjection> countLeadsByStatus(@Param("ownerId") Long ownerId);

	@Query("""
			SELECT lead.source AS source,
			       COUNT(lead) AS total,
			       SUM(CASE WHEN lead.status = :closedStatus THEN 1 ELSE 0 END) AS closedLeads
			FROM Lead lead
			WHERE lead.owner.id = :ownerId
			GROUP BY lead.source
			""")
	List<LeadSourceConversionProjection> countConversionBySource(
			@Param("ownerId") Long ownerId,
			@Param("closedStatus") LeadStatus closedStatus
	);

	@Query(value = """
			SELECT AVG(EXTRACT(EPOCH FROM (closed_at - created_at)) / 86400)
			FROM leads
			WHERE owner_id = :ownerId
			  AND status = 'CLOSED'
			  AND closed_at IS NOT NULL
			""", nativeQuery = true)
	BigDecimal averageDaysToCloseByOwnerId(@Param("ownerId") Long ownerId);

	@Query("""
			SELECT lead FROM Lead lead
			WHERE lead.staleLead = false
			  AND lead.status NOT IN :excludedStatuses
			  AND COALESCE(
			        (SELECT MAX(interaction.interactionDate) FROM Interaction interaction WHERE interaction.lead = lead),
			        lead.createdAt
			      ) <= :cutoff
			""")
	List<Lead> findLeadsEligibleForStaleFlag(
			@Param("cutoff") LocalDateTime cutoff,
			@Param("excludedStatuses") Collection<LeadStatus> excludedStatuses
	);
}
