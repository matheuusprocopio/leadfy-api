package com.leadfy.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.entity.AiLeadRecommendation;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.service.NormalizedAiLeadInsight;
import java.time.Instant;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

// Runs LeadRepository's queries against a real Postgres instance, unlike the mocked unit tests.
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class LeadRepositoryIT extends AbstractIntegrationTest {

	@Autowired
	private LeadRepository leadRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findByOwnerIdShouldNotReturnLeadsFromAnotherOwner() {
		User ownerA = persistUser("owner-a@leadfy.com");
		User ownerB = persistUser("owner-b@leadfy.com");
		persistLead(ownerA, "Lead A");
		persistLead(ownerB, "Lead B");

		Page<Lead> ownerALeads = leadRepository.findByOwnerId(ownerA.getId(), PageRequest.of(0, 20));

		assertThat(ownerALeads.getContent()).extracting(Lead::getName).containsExactly("Lead A");
		assertThat(ownerALeads.getTotalElements()).isEqualTo(1);
	}

	@Test
	void findByIdAndOwnerIdShouldRejectLeadFromAnotherOwner() {
		User ownerA = persistUser("owner-c@leadfy.com");
		User ownerB = persistUser("owner-d@leadfy.com");
		Lead lead = persistLead(ownerA, "Lead C");

		Optional<Lead> asOwnerA = leadRepository.findByIdAndOwnerId(lead.getId(), ownerA.getId());
		Optional<Lead> asOwnerB = leadRepository.findByIdAndOwnerId(lead.getId(), ownerB.getId());

		assertThat(asOwnerA).isPresent();
		assertThat(asOwnerB).isEmpty();
	}

	@Test
	void findLeadsEligibleForStaleFlagShouldOnlyReturnLeadsPastTheThreshold() {
		User owner = persistUser("owner-stale@leadfy.com");
		LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

		Lead neverContacted = persistLead(owner, "Never contacted");
		backdateCreatedAt(neverContacted, LocalDateTime.now().minusDays(10));

		Lead staleInteraction = persistLead(owner, "Old interaction");
		persistInteraction(staleInteraction, LocalDateTime.now().minusDays(9));

		Lead freshInteraction = persistLead(owner, "Recently contacted");
		persistInteraction(freshInteraction, LocalDateTime.now().minusDays(1));

		Lead closedLead = persistLead(owner, "Closed deal");
		backdateCreatedAt(closedLead, LocalDateTime.now().minusDays(30));
		closedLead.updateStatus(LeadStatus.CLOSED);
		leadRepository.save(closedLead);

		entityManager.flush();
		entityManager.clear();

		List<Lead> eligible = leadRepository.findLeadsEligibleForStaleFlag(
				cutoff,
				List.of(LeadStatus.CLOSED, LeadStatus.LOST)
		);

		assertThat(eligible)
				.extracting(Lead::getName)
				.containsExactlyInAnyOrder("Never contacted", "Old interaction");
	}

	@Test
	void findLeadsEligibleForAiRecommendationsShouldPrioritizeStaleAndSkipFreshRecommendations() {
		User owner = persistUser("owner-ai-refresh@leadfy.com");
		Instant freshAfter = Instant.now().minusSeconds(3600);

		Lead staleLead = persistLead(owner, "Stale lead");
		staleLead.markAsStale();
		leadRepository.save(staleLead);

		Lead freshRecommendation = persistLead(owner, "Fresh recommendation");
		persistRecommendation(freshRecommendation, Instant.now());

		Lead regularLead = persistLead(owner, "Regular lead");

		Lead closedLead = persistLead(owner, "Closed lead");
		closedLead.updateStatus(LeadStatus.CLOSED);
		leadRepository.save(closedLead);

		entityManager.flush();
		entityManager.clear();

		List<Lead> eligible = leadRepository.findLeadsEligibleForAiRecommendations(
				freshAfter,
				List.of(LeadStatus.CLOSED, LeadStatus.LOST),
				PageRequest.of(0, 10)
		);

		assertThat(eligible)
				.extracting(Lead::getName)
				.containsExactly("Stale lead", "Regular lead");
	}

	private User persistUser(String email) {
		return userRepository.save(new User("Jane Doe", email, "encoded-password"));
	}

	private Lead persistLead(User owner, String name) {
		String email = name.toLowerCase().replace(" ", ".") + "@acme.com";
		Lead lead = new Lead(name, "Acme Inc", email, null, LeadSource.WEBSITE, null, owner);
		return leadRepository.save(lead);
	}

	private void persistInteraction(Lead lead, LocalDateTime interactionDate) {
		entityManager.persist(new Interaction(InteractionType.CALL, "Follow up", interactionDate, lead));
	}

	private void persistRecommendation(Lead lead, Instant generatedAt) {
		entityManager.persist(new AiLeadRecommendation(
				lead,
				"[]",
				"[]",
				new NormalizedAiLeadInsight(
						80,
						"Resumo",
						List.of(),
						List.of(),
						"Agir",
						"Mensagem",
						"HIGH"
				),
				generatedAt
		));
	}

	// createdAt is updatable = false, so Hibernate ignores it on entity updates; native SQL is required to backdate it.
	private void backdateCreatedAt(Lead lead, LocalDateTime createdAt) {
		entityManager.flush();
		entityManager.createNativeQuery("UPDATE leads SET created_at = :createdAt WHERE id = :id")
				.setParameter("createdAt", createdAt)
				.setParameter("id", lead.getId())
				.executeUpdate();
		entityManager.clear();
	}
}
