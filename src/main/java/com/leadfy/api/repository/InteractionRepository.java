package com.leadfy.api.repository;

import com.leadfy.api.entity.Interaction;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {

	Page<Interaction> findByLeadIdAndLeadOwnerId(Long leadId, Long ownerId, Pageable pageable);

	Optional<Interaction> findByIdAndLeadIdAndLeadOwnerId(Long id, Long leadId, Long ownerId);
}
