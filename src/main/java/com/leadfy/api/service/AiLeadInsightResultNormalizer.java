package com.leadfy.api.service;

import com.leadfy.api.client.AiLeadInsightResult;
import com.leadfy.api.exception.AiResponseParsingException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AiLeadInsightResultNormalizer {

	private static final int GENERATED_TEXT_MAX_LENGTH = 1200;

	public NormalizedAiLeadInsight normalize(AiLeadInsightResult result) {
		if (result == null) {
			throw invalidAiResponse("AI response body is empty");
		}

		return new NormalizedAiLeadInsight(
				validatePriorityScore(result.priorityScore()),
				requireGeneratedText(result.summary(), "summary"),
				cleanGeneratedList(result.conversionSignals()),
				cleanGeneratedList(result.riskSignals()),
				requireGeneratedText(result.nextBestAction(), "nextBestAction"),
				requireGeneratedText(result.suggestedMessage(), "suggestedMessage"),
				validateConfidence(result.confidence())
		);
	}

	private Integer validatePriorityScore(Integer priorityScore) {
		if (priorityScore == null || priorityScore < 0 || priorityScore > 100) {
			throw invalidAiResponse("priorityScore must be between 0 and 100");
		}

		return priorityScore;
	}

	private String validateConfidence(String confidence) {
		String normalizedConfidence = requireGeneratedText(confidence, "confidence").toUpperCase(Locale.ROOT);

		if (!List.of("LOW", "MEDIUM", "HIGH").contains(normalizedConfidence)) {
			throw invalidAiResponse("confidence must be LOW, MEDIUM or HIGH");
		}

		return normalizedConfidence;
	}

	private List<String> cleanGeneratedList(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return values.stream()
				.map(value -> cleanText(value, GENERATED_TEXT_MAX_LENGTH))
				.filter(value -> value != null && !value.isBlank())
				.toList();
	}

	private String requireGeneratedText(String value, String fieldName) {
		String cleanedValue = cleanText(value, GENERATED_TEXT_MAX_LENGTH);

		if (cleanedValue == null) {
			throw invalidAiResponse(fieldName + " is required");
		}

		return cleanedValue;
	}

	private String cleanText(String value, int maxLength) {
		if (value == null || value.trim().isEmpty()) {
			return null;
		}

		String normalized = value.trim().replaceAll("\\s+", " ");

		if (normalized.length() <= maxLength) {
			return normalized;
		}

		return normalized.substring(0, maxLength - 3) + "...";
	}

	private AiResponseParsingException invalidAiResponse(String message) {
		return new AiResponseParsingException(new IllegalArgumentException(message));
	}
}
