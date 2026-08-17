package com.leadfy.api.exception;

public class AiClientException extends RuntimeException {

	private static final String DEFAULT_MESSAGE = "AI insights are temporarily unavailable.";

	public AiClientException() {
		super(DEFAULT_MESSAGE);
	}

	public AiClientException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
