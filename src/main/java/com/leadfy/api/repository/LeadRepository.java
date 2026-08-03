package com.leadfy.api.repository;

import com.leadfy.api.entity.Lead;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {

	List<Lead> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

	Optional<Lead> findByIdAndOwnerId(Long id, Long ownerId);
}
