package com.leadfy.api.exception;

public class AiResponseParsingException extends RuntimeException {

	private static final String DEFAULT_MESSAGE = "AI insights are temporarily unavailable.";

	public AiResponseParsingException(Throwable cause) {
		super(DEFAULT_MESSAGE, cause);
	}
}
