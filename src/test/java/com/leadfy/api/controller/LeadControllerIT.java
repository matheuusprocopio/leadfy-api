package com.leadfy.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.enums.LeadSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LeadControllerIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void findAllShouldRejectUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/leads"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void ownerShouldNotAccessLeadFromAnotherOwner() throws Exception {
		String ownerAToken = registerAndGetToken("owner-a@leadfy.com");
		String ownerBToken = registerAndGetToken("owner-b@leadfy.com");
		Long leadId = createLead(ownerAToken, "Owner A Lead");

		mockMvc.perform(get("/api/leads/" + leadId).header("Authorization", "Bearer " + ownerAToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/leads/" + leadId).header("Authorization", "Bearer " + ownerBToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void findAllShouldReturnOnlyLeadsOwnedByTheAuthenticatedUser() throws Exception {
		String ownerAToken = registerAndGetToken("owner-a-list@leadfy.com");
		String ownerBToken = registerAndGetToken("owner-b-list@leadfy.com");

		createLead(ownerAToken, "Owner A Lead");
		createLead(ownerBToken, "Owner B Lead");

		mockMvc.perform(get("/api/leads").header("Authorization", "Bearer " + ownerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].name").value("Owner A Lead"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void updateStatusShouldRejectSkippingFunnelSteps() throws Exception {
		String token = registerAndGetToken("transition-test@leadfy.com");
		Long leadId = createLead(token, "Skip Funnel Lead");

		mockMvc.perform(patch("/api/leads/" + leadId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"status\":\"CLOSED\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_LEAD_STATUS_TRANSITION"));
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
