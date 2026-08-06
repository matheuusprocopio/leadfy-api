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
import com.leadfy.api.dto.request.UpdateLeadStatusRequest;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MetricsControllerIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void overviewShouldAggregateOnlyTheAuthenticatedOwnersLeads() throws Exception {
		String token = registerAndGetToken("metrics-owner@leadfy.com");
		String otherToken = registerAndGetToken("metrics-other@leadfy.com");

		Long closedLeadId = createLead(token, "Closed Deal", LeadSource.LINKEDIN);
		advanceStatus(token, closedLeadId, LeadStatus.CONTACT_MADE);
		advanceStatus(token, closedLeadId, LeadStatus.PROPOSAL_SENT);
		advanceStatus(token, closedLeadId, LeadStatus.NEGOTIATION);
		advanceStatus(token, closedLeadId, LeadStatus.CLOSED);

		createLead(token, "Open Deal", LeadSource.LINKEDIN);

		Long lostLeadId = createLead(token, "Lost Deal", LeadSource.REFERRAL);
		advanceStatus(token, lostLeadId, LeadStatus.LOST);

		// Belongs to a different owner and must not leak into the metrics above.
		createLead(otherToken, "Someone Else's Deal", LeadSource.WEBSITE);

		mockMvc.perform(get("/api/metrics/overview").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalLeads").value(3))
				.andExpect(jsonPath("$.openLeads").value(1))
				.andExpect(jsonPath("$.closedLeads").value(1))
				.andExpect(jsonPath("$.lostLeads").value(1))
				.andExpect(jsonPath("$.conversionRatePercentage").value(33.33))
				.andExpect(jsonPath("$.averageDaysToClose").isNotEmpty())
				.andExpect(jsonPath("$.leadsByStatus[?(@.status=='NEW')].total").value(1))
				.andExpect(jsonPath("$.leadsByStatus[?(@.status=='CLOSED')].total").value(1))
				.andExpect(jsonPath("$.leadsByStatus[?(@.status=='LOST')].total").value(1))
				.andExpect(jsonPath("$.conversionBySource[?(@.source=='LINKEDIN')].totalLeads").value(2))
				.andExpect(jsonPath("$.conversionBySource[?(@.source=='LINKEDIN')].closedLeads").value(1))
				.andExpect(jsonPath("$.conversionBySource[?(@.source=='REFERRAL')].totalLeads").value(1))
				.andExpect(jsonPath("$.conversionBySource[?(@.source=='REFERRAL')].closedLeads").value(0));
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

	private Long createLead(String token, String name, LeadSource source) throws Exception {
		CreateLeadRequest request = new CreateLeadRequest(
				name,
				"Acme Inc",
				name.toLowerCase().replace(" ", ".").replace("'", "") + "@acme.com",
				null,
				source,
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

	private void advanceStatus(String token, Long leadId, LeadStatus status) throws Exception {
		mockMvc.perform(patch("/api/leads/" + leadId + "/status")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new UpdateLeadStatusRequest(status))))
				.andExpect(status().isOk());
	}
}
