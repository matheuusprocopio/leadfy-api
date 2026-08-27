package com.leadfy.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.AbstractIntegrationTest;
import com.leadfy.api.client.AiClient;
import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.dto.request.CreateLeadRequest;
import com.leadfy.api.dto.request.RegisterRequest;
import com.leadfy.api.dto.request.UpdateAiRecommendationFeedbackRequest;
import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.service.AiLeadInsightContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AiLeadRecommendationControllerIT extends AbstractIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AiClient aiClient;

	@Test
	void findActiveShouldRejectUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/ai/recommendations"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void generateShouldPersistRecommendationAndListItForOwner() throws Exception {
		String token = registerAndGetToken("ai-recommendations@leadfy.com");
		Long leadId = createLead(token, "Cliente Prioritario");
		when(aiClient.generateLeadInsight(any(AiLeadInsightContext.class))).thenReturn(aiResult());

		String response = mockMvc.perform(post("/api/ai/recommendations/leads/" + leadId)
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leadId").value(leadId))
				.andExpect(jsonPath("$.leadName").value("Cliente Prioritario"))
				.andExpect(jsonPath("$.priorityScore").value(88))
				.andExpect(jsonPath("$.conversionSignals[0]").value("Proposta recente"))
				.andExpect(jsonPath("$.riskSignals[0]").value("Sem resposta nos ultimos dias"))
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn().getResponse().getContentAsString();

		Long recommendationId = objectMapper.readTree(response).get("id").asLong();

		mockMvc.perform(get("/api/ai/recommendations")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].id").value(recommendationId))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void feedbackShouldRejectRecommendationFromAnotherOwner() throws Exception {
		String ownerAToken = registerAndGetToken("ai-owner-a@leadfy.com");
		String ownerBToken = registerAndGetToken("ai-owner-b@leadfy.com");
		Long leadId = createLead(ownerAToken, "Owner A Priority");
		when(aiClient.generateLeadInsight(any(AiLeadInsightContext.class))).thenReturn(aiResult());

		String response = mockMvc.perform(post("/api/ai/recommendations/leads/" + leadId)
						.header("Authorization", "Bearer " + ownerAToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		Long recommendationId = objectMapper.readTree(response).get("id").asLong();
		UpdateAiRecommendationFeedbackRequest request = new UpdateAiRecommendationFeedbackRequest(
				AiRecommendationStatus.ACTIONED,
				true
		);

		mockMvc.perform(patch("/api/ai/recommendations/" + recommendationId + "/feedback")
						.header("Authorization", "Bearer " + ownerBToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
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
				"Lead interessado em automacao"
		);

		String response = mockMvc.perform(post("/api/leads")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(response).get("id").asLong();
	}

	private AiLeadInsightResult aiResult() {
		return new AiLeadInsightResult(
				88,
				"Lead com prioridade alta para retomada.",
				List.of("Proposta recente"),
				List.of("Sem resposta nos ultimos dias"),
				"Enviar follow-up consultivo ainda hoje.",
				"Oi, tudo bem? Posso ajudar com alguma duvida sobre a proposta?",
				"HIGH"
		);
	}
}
