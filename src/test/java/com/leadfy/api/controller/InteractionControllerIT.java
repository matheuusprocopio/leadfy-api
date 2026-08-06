package com.leadfy.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.dto.request.CreateInteractionRequest;
import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.enums.InteractionType;
import com.leadfy.api.enums.LeadSource;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InteractionControllerIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createShouldRegisterInteractionForOwnedLead() throws Exception {
		String token = registerAndGetToken("interaction-owner@leadfy.com");
		Long leadId = createLead(token, "Interaction Lead");

		CreateInteractionRequest request = new CreateInteractionRequest(
				InteractionType.CALL,
				"Discovery call",
				LocalDateTime.now().minusHours(2)
		);

		mockMvc.perform(post("/api/leads/" + leadId + "/interactions")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.leadId").value(leadId))
				.andExpect(jsonPath("$.type").value("CALL"))
				.andExpect(jsonPath("$.description").value("Discovery call"));
	}

	@Test
	void findAllShouldRejectAccessToInteractionsFromAnotherOwnersLead() throws Exception {
		String ownerAToken = registerAndGetToken("interaction-a@leadfy.com");
		String ownerBToken = registerAndGetToken("interaction-b@leadfy.com");
		Long leadId = createLead(ownerAToken, "Owner A Interaction Lead");

		mockMvc.perform(get("/api/leads/" + leadId + "/interactions")
						.header("Authorization", "Bearer " + ownerBToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void findAllShouldRejectUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/leads/1/interactions"))
				.andExpect(status().isUnauthorized());
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
}
