package com.leadfy.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.entity.Interaction;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class InteractionRepositoryIT extends AbstractIntegrationTest {

	@Autowired
	private InteractionRepository interactionRepository;

	@Autowired
	private LeadRepository leadRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByLeadIdAndLeadOwnerIdShouldRejectInteractionsFromAnotherOwnersLead() {
		User ownerA = persistUser("owner-interactions-a@leadfy.com");
		User ownerB = persistUser("owner-interactions-b@leadfy.com");
		Lead leadA = persistLead(ownerA, "Lead A");
		persistInteraction(leadA, InteractionType.CALL, LocalDateTime.now().minusDays(1));

		List<Interaction> asOwnerA = interactionRepository
				.findByLeadIdAndLeadOwnerIdOrderByInteractionDateDescCreatedAtDesc(leadA.getId(), ownerA.getId());
		List<Interaction> asOwnerB = interactionRepository
				.findByLeadIdAndLeadOwnerIdOrderByInteractionDateDescCreatedAtDesc(leadA.getId(), ownerB.getId());

		assertThat(asOwnerA).hasSize(1);
		assertThat(asOwnerB).isEmpty();
	}

	@Test
	void findByLeadIdAndLeadOwnerIdShouldOrderByMostRecentInteractionFirst() {
		User owner = persistUser("owner-interactions-order@leadfy.com");
		Lead lead = persistLead(owner, "Lead Order");
		persistInteraction(lead, InteractionType.EMAIL, LocalDateTime.now().minusDays(5));
		Interaction mostRecent = persistInteraction(lead, InteractionType.CALL, LocalDateTime.now().minusDays(1));

		List<Interaction> interactions = interactionRepository
				.findByLeadIdAndLeadOwnerIdOrderByInteractionDateDescCreatedAtDesc(lead.getId(), owner.getId());

		assertThat(interactions).hasSize(2);
		assertThat(interactions.get(0).getId()).isEqualTo(mostRecent.getId());
	}

	@Test
	void findByIdAndLeadIdAndLeadOwnerIdShouldRejectInteractionFromAnotherOwner() {
		User ownerA = persistUser("owner-interactions-c@leadfy.com");
		User ownerB = persistUser("owner-interactions-d@leadfy.com");
		Lead lead = persistLead(ownerA, "Lead C");
		Interaction interaction = persistInteraction(lead, InteractionType.MEETING, LocalDateTime.now().minusHours(3));

		Optional<Interaction> asOwnerA = interactionRepository
				.findByIdAndLeadIdAndLeadOwnerId(interaction.getId(), lead.getId(), ownerA.getId());
		Optional<Interaction> asOwnerB = interactionRepository
				.findByIdAndLeadIdAndLeadOwnerId(interaction.getId(), lead.getId(), ownerB.getId());

		assertThat(asOwnerA).isPresent();
		assertThat(asOwnerB).isEmpty();
	}

	private User persistUser(String email) {
		return userRepository.save(new User("Jane Doe", email, "encoded-password"));
	}

	private Lead persistLead(User owner, String name) {
		String email = name.toLowerCase().replace(" ", ".") + "@acme.com";
		Lead lead = new Lead(name, "Acme Inc", email, null, LeadSource.WEBSITE, null, owner);
		return leadRepository.save(lead);
	}

	private Interaction persistInteraction(Lead lead, InteractionType type, LocalDateTime interactionDate) {
		return interactionRepository.save(new Interaction(type, "Follow up", interactionDate, lead));
	}
}
