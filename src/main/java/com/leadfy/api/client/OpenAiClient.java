package com.leadfy.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadfy.api.exception.AiClientException;
import com.leadfy.api.exception.AiInsightsUnavailableException;
import com.leadfy.api.exception.AiResponseParsingException;
import com.leadfy.api.service.AiLeadInsightContext;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiClient implements AiClient {

	private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";
	private static final String DEFAULT_MODEL = "gpt-4o-mini";
	private static final int DEFAULT_TIMEOUT_SECONDS = 10;
	private static final int MAX_OUTPUT_TOKENS = 800;
	private static final double TEMPERATURE = 0.2;

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	@Autowired
	public OpenAiClient(
			RestClient.Builder restClientBuilder,
			ObjectMapper objectMapper,
			@Value("${leadfy.ai.openai.api-key:}") String apiKey,
			@Value("${leadfy.ai.openai.model:gpt-4o-mini}") String model,
			@Value("${leadfy.ai.openai.timeout-seconds:10}") String timeoutSeconds
	) {
		this(
				restClientBuilder
						.baseUrl(OPENAI_BASE_URL)
						.requestFactory(createRequestFactory(timeoutSeconds))
						.build(),
				objectMapper,
				apiKey,
				model
		);
	}

	OpenAiClient(RestClient restClient, ObjectMapper objectMapper, String apiKey, String model) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.apiKey = normalize(apiKey);
		this.model = hasText(model) ? model.trim() : DEFAULT_MODEL;
	}

	@Override
	public AiLeadInsightResult generateLeadInsight(AiLeadInsightContext context) {
		if (!hasText(apiKey)) {
			throw new AiInsightsUnavailableException();
		}

		try {
			String responseBody = restClient.post()
					.uri("/responses")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(buildRequestBody(context))
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new AiClientException();
					})
					.body(String.class);

			return parseResponse(responseBody);
		} catch (AiClientException | AiInsightsUnavailableException | AiResponseParsingException exception) {
			throw exception;
		} catch (ResourceAccessException exception) {
			throw new AiClientException(exception);
		} catch (RestClientException exception) {
			throw new AiClientException(exception);
		}
	}

	private Map<String, Object> buildRequestBody(AiLeadInsightContext context) {
		return Map.of(
				"model", model,
				"input", List.of(
						Map.of(
								"role", "system",
								"content", buildSystemPrompt()
						),
						Map.of(
								"role", "user",
								"content", buildUserPrompt(context)
						)
				),
				"temperature", TEMPERATURE,
				"max_output_tokens", MAX_OUTPUT_TOKENS,
				"text", Map.of(
						"format", Map.of(
								"type", "json_schema",
								"name", "leadfy_ai_insight",
								"description", "Commercial insight for a CRM lead.",
								"schema", buildInsightSchema(),
								"strict", true
						)
				)
		);
	}

	private String buildSystemPrompt() {
		return """
				Voce e o Leadfy AI Coach, um assistente comercial para freelancers que analisa leads de CRM.
				Responda em portugues brasileiro e exclusivamente com JSON valido no schema solicitado.
				Use apenas as informacoes fornecidas. Nao invente fatos, valores, datas ou intencoes.
				Se os dados forem insuficientes, diga isso no resumo e reduza a confidence.
				Gere recomendacoes objetivas, curtas e acionaveis.
				Nao prometa fechamento de venda.
				Nao use linguagem agressiva, insistente ou manipulativa.
				A suggestedMessage deve ser curta, profissional e adequada para WhatsApp ou e-mail.
				""";
	}

	private String buildUserPrompt(AiLeadInsightContext context) {
		return "Analise os dados comerciais abaixo e gere uma recomendacao pratica. "
				+ "Use apenas as informacoes fornecidas. Se os dados forem insuficientes, "
				+ "diga isso no resumo e reduza a confidence. Responda exclusivamente no schema JSON solicitado.\n\n"
				+ "Dados comerciais:\n"
				+ serializeContext(context);
	}

	private String serializeContext(AiLeadInsightContext context) {
		try {
			return objectMapper.writeValueAsString(context);
		} catch (JsonProcessingException exception) {
			throw new AiResponseParsingException(exception);
		}
	}

	private Map<String, Object> buildInsightSchema() {
		return Map.of(
				"type", "object",
				"additionalProperties", false,
				"properties", Map.of(
						"priorityScore", Map.of("type", "integer"),
						"summary", Map.of("type", "string"),
						"conversionSignals", Map.of(
								"type", "array",
								"items", Map.of("type", "string")
						),
						"riskSignals", Map.of(
								"type", "array",
								"items", Map.of("type", "string")
						),
						"nextBestAction", Map.of("type", "string"),
						"suggestedMessage", Map.of("type", "string"),
						"confidence", Map.of(
								"type", "string",
								"enum", List.of("LOW", "MEDIUM", "HIGH")
						)
				),
				"required", List.of(
						"priorityScore",
						"summary",
						"conversionSignals",
						"riskSignals",
						"nextBestAction",
						"suggestedMessage",
						"confidence"
				)
		);
	}

	private AiLeadInsightResult parseResponse(String responseBody) {
		if (!hasText(responseBody)) {
			throw invalidAiResponse("OpenAI response body is empty");
		}

		try {
			JsonNode root = objectMapper.readTree(responseBody);
			String outputText = extractOutputText(root);
			return objectMapper.readValue(outputText, AiLeadInsightResult.class);
		} catch (JsonProcessingException exception) {
			throw new AiResponseParsingException(exception);
		}
	}

	private String extractOutputText(JsonNode root) {
		if (root.hasNonNull("output_text")) {
			String outputText = root.get("output_text").asText();
			if (hasText(outputText)) {
				return outputText;
			}
		}

		JsonNode output = root.path("output");
		if (output.isArray()) {
			for (JsonNode outputItem : output) {
				JsonNode content = outputItem.path("content");
				if (!content.isArray()) {
					continue;
				}

				for (JsonNode contentItem : content) {
					JsonNode text = contentItem.get("text");
					if (text != null && hasText(text.asText())) {
						return text.asText();
					}
				}
			}
		}

		throw invalidAiResponse("OpenAI response does not include output text");
	}

	private static SimpleClientHttpRequestFactory createRequestFactory(String timeoutSeconds) {
		int normalizedTimeoutSeconds = parseTimeoutSeconds(timeoutSeconds);
		Duration timeout = Duration.ofSeconds(normalizedTimeoutSeconds);

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(timeout);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}

	private static int parseTimeoutSeconds(String timeoutSeconds) {
		if (!hasText(timeoutSeconds)) {
			return DEFAULT_TIMEOUT_SECONDS;
		}

		try {
			int parsedTimeoutSeconds = Integer.parseInt(timeoutSeconds.trim());
			return parsedTimeoutSeconds > 0 ? parsedTimeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
		} catch (NumberFormatException exception) {
			return DEFAULT_TIMEOUT_SECONDS;
		}
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim();
	}

	private static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private AiResponseParsingException invalidAiResponse(String message) {
		return new AiResponseParsingException(new IllegalArgumentException(message));
	}
}
