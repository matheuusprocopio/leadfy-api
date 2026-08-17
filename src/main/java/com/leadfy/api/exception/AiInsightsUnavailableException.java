package com.leadfy.api.exception;

public class AiInsightsUnavailableException extends RuntimeException {

	public AiInsightsUnavailableException() {
		super("AI insights are temporarily unavailable.");
	}
}
