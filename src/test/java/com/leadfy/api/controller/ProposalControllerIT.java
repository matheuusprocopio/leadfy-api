package com.leadfy.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.CreateProposalRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.dto.request.UpdateProposalStatusRequest;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.ProposalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProposalControllerIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createShouldRegisterProposalWithSentStatus() throws Exception {
		String token = registerAndGetToken("proposal-owner@leadfy.com");
		Long leadId = createLead(token, "Proposal Lead");

		mockMvc.perform(post("/api/leads/" + leadId + "/proposals")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new CreateProposalRequest(BigDecimal.valueOf(2500), LocalDate.now()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SENT"))
				.andExpect(jsonPath("$.amount").value(2500));
	}

	@Test
	void updateStatusShouldAcceptValidTransitionToAccepted() throws Exception {
		String token = registerAndGetToken("proposal-accept@leadfy.com");
		Long leadId = createLead(token, "Accepted Proposal Lead");
		Long proposalId = createProposal(token, leadId);

		mockMvc.perform(patch("/api/leads/" + leadId + "/proposals/" + proposalId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateProposalStatusRequest(ProposalStatus.ACCEPTED, LocalDate.now()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACCEPTED"))
				.andExpect(jsonPath("$.respondedAt").exists());
	}

	@Test
	void updateStatusShouldRejectTransitionOutOfAFinalStatus() throws Exception {
		String token = registerAndGetToken("proposal-final@leadfy.com");
		Long leadId = createLead(token, "Final Proposal Lead");
		Long proposalId = createProposal(token, leadId);

		mockMvc.perform(patch("/api/leads/" + leadId + "/proposals/" + proposalId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateProposalStatusRequest(ProposalStatus.ACCEPTED, LocalDate.now()))))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/leads/" + leadId + "/proposals/" + proposalId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(
								new UpdateProposalStatusRequest(ProposalStatus.REJECTED, LocalDate.now()))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_PROPOSAL_STATUS_TRANSITION"));
	}

	@Test
	void findAllShouldRejectAccessToProposalsFromAnotherOwnersLead() throws Exception {
		String ownerAToken = registerAndGetToken("proposal-a@leadfy.com");
		String ownerBToken = registerAndGetToken("proposal-b@leadfy.com");
		Long leadId = createLead(ownerAToken, "Owner A Proposal Lead");

		mockMvc.perform(get("/api/leads/" + leadId + "/proposals")
						.header("Authorization", "Bearer " + ownerBToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	private String registerAndGetToken(String email) throws Exception {
		RegisterRequest request = new RegisterRequest("Jane Doe", email, "Secret123!");

		String response = mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).get("token").asText();
	}

	private Long createLead(String token, String name) throws Exception {
		CreateLeadRequest request = new CreateLeadRequest(
				name,
				"Acme Inc",
				name.toLowerCase().replace(" ", ".") + "@acme.com",
				null,
				LeadSource.WEBSITE,
				null
		);

		String response = mockMvc.perform(post("/api/leads")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}

	private Long createProposal(String token, Long leadId) throws Exception {
		CreateProposalRequest request = new CreateProposalRequest(BigDecimal.valueOf(3000), LocalDate.now());

		String response = mockMvc.perform(post("/api/leads/" + leadId + "/proposals")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}
}
