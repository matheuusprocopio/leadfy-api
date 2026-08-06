package com.leadfy.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.entity.Lead;
import com.leadfy.api.entity.Proposal;
import com.leadfy.api.entity.User;
import com.leadfy.api.enums.LeadSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class ProposalRepositoryIT extends AbstractIntegrationTest {

	@Autowired
	private ProposalRepository proposalRepository;

	@Autowired
	private LeadRepository leadRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findByLeadIdAndLeadOwnerIdShouldRejectProposalsFromAnotherOwnersLead() {
		User ownerA = persistUser("owner-proposals-a@leadfy.com");
		User ownerB = persistUser("owner-proposals-b@leadfy.com");
		Lead leadA = persistLead(ownerA, "Lead A");
		persistProposal(leadA, LocalDate.now().minusDays(2));

		Page<Proposal> asOwnerA = proposalRepository
				.findByLeadIdAndLeadOwnerId(leadA.getId(), ownerA.getId(), PageRequest.of(0, 20));
		Page<Proposal> asOwnerB = proposalRepository
				.findByLeadIdAndLeadOwnerId(leadA.getId(), ownerB.getId(), PageRequest.of(0, 20));

		assertThat(asOwnerA.getContent()).hasSize(1);
		assertThat(asOwnerB.getContent()).isEmpty();
	}

	@Test
	void findByIdAndLeadIdAndLeadOwnerIdShouldRejectProposalFromAnotherOwner() {
		User ownerA = persistUser("owner-proposals-c@leadfy.com");
		User ownerB = persistUser("owner-proposals-d@leadfy.com");
		Lead lead = persistLead(ownerA, "Lead C");
		Proposal proposal = persistProposal(lead, LocalDate.now().minusDays(1));

		Optional<Proposal> asOwnerA = proposalRepository
				.findByIdAndLeadIdAndLeadOwnerId(proposal.getId(), lead.getId(), ownerA.getId());
		Optional<Proposal> asOwnerB = proposalRepository
				.findByIdAndLeadIdAndLeadOwnerId(proposal.getId(), lead.getId(), ownerB.getId());

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

	private Proposal persistProposal(Lead lead, LocalDate sentAt) {
		return proposalRepository.save(new Proposal(BigDecimal.valueOf(1000), sentAt, lead));
	}
}
