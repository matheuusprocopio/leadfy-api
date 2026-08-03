package com.leadfy.api.repository;

import com.leadfy.api.entity.Interaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

	List<Interaction> findByLeadIdAndLeadOwnerIdOrderByInteractionDateDescCreatedAtDesc(Long leadId, Long ownerId);

	Optional<Interaction> findByIdAndLeadIdAndLeadOwnerId(Long id, Long leadId, Long ownerId);
}
