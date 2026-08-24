package com.leadfy.api.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
import com.leadfy.api.exception.AiClientException;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.AiResponseParsingException;
import com.leadfy.api.service.AiLeadInsightContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

	@Test
	void generateLeadInsightShouldParseStructuredOutputText() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiClient client = new OpenAiClient(
				builder.baseUrl("https://api.openai.com/v1").build(),
				objectMapper,
				"test-key",
				"test-model"
		);
		String providerResponse = """
				{
				  "output_text": "{\\"priorityScore\\":78,\\"summary\\":\\"Lead engajado\\",\\"conversionSignals\\":[\\"Respondeu rapido\\"],\\"riskSignals\\":[\\"Sem proposta aceita\\"],\\"nextBestAction\\":\\"Enviar follow-up\\",\\"suggestedMessage\\":\\"Oi, posso tirar alguma duvida?\\",\\"confidence\\":\\"HIGH\\"}"
				}
				""";

		server.expect(requestTo("https://api.openai.com/v1/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"test-model\"")))
				.andRespond(withSuccess(providerResponse, MediaType.APPLICATION_JSON));

		AiLeadInsightResult result = client.generateLeadInsight(context());

		assertThat(result.priorityScore()).isEqualTo(78);
		assertThat(result.summary()).isEqualTo("Lead engajado");
		assertThat(result.conversionSignals()).containsExactly("Respondeu rapido");
		assertThat(result.riskSignals()).containsExactly("Sem proposta aceita");
		assertThat(result.nextBestAction()).isEqualTo("Enviar follow-up");
		assertThat(result.suggestedMessage()).isEqualTo("Oi, posso tirar alguma duvida?");
		assertThat(result.confidence()).isEqualTo("HIGH");
		server.verify();
	}

	@Test
	void generateLeadInsightShouldFailWhenApiKeyIsMissing() {
		OpenAiClient client = new OpenAiClient(RestClient.builder(), objectMapper, " ", "test-model", "5");

		assertThatThrownBy(() -> client.generateLeadInsight(context()))
				.isInstanceOf(AiInsightsUnavailableException.class);
	}

	@Test
	void constructorShouldUseDefaultTimeoutWhenPropertyIsBlankOrInvalid() {
		assertThatCode(() -> new OpenAiClient(RestClient.builder(), objectMapper, "test-key", "test-model", " "))
				.doesNotThrowAnyException();
		assertThatCode(() -> new OpenAiClient(RestClient.builder(), objectMapper, "test-key", "test-model", "invalid"))
				.doesNotThrowAnyException();
	}

	@Test
	void generateLeadInsightShouldWrapProviderErrors() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiClient client = new OpenAiClient(
				builder.baseUrl("https://api.openai.com/v1").build(),
				objectMapper,
				"test-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withBadRequest());

		assertThatThrownBy(() -> client.generateLeadInsight(context()))
				.isInstanceOf(AiClientException.class);
		server.verify();
	}

	@Test
	void generateLeadInsightShouldRejectInvalidProviderResponse() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		OpenAiClient client = new OpenAiClient(
				builder.baseUrl("https://api.openai.com/v1").build(),
				objectMapper,
				"test-key",
				"test-model"
		);

		server.expect(requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess("{\"output\":[]}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.generateLeadInsight(context()))
				.isInstanceOf(AiResponseParsingException.class);
		server.verify();
	}

	private AiLeadInsightContext context() {
		return new AiLeadInsightContext(
				"Maria Cliente",
				"Acme Inc",
				LeadSource.WEBSITE,
				LeadStatus.CONTACT_MADE,
				false,
				LocalDateTime.of(2026, 8, 20, 9, 0),
				LocalDateTime.of(2026, 8, 23, 11, 30),
				null,
				"Quer automatizar captacao.",
				List.of(),
				List.of()
		);
	}
}
