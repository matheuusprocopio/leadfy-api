package com.leadfy.api.exception;

import com.leadfy.api.dto.response.ErrorResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EmailAlreadyRegisteredException.class)
	public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of("EMAIL_ALREADY_REGISTERED", exception.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
		return ResponseEntity
				.status(HttpStatus.UNAUTHORIZED)
				.body(ErrorResponse.of("INVALID_CREDENTIALS", exception.getMessage()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
		return ResponseEntity
				.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.of("RESOURCE_NOT_FOUND", exception.getMessage()));
	}

	@ExceptionHandler(InvalidLeadStatusTransitionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidLeadStatusTransition(
			InvalidLeadStatusTransitionException exception
	) {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of("INVALID_LEAD_STATUS_TRANSITION", exception.getMessage()));
	}

	@ExceptionHandler(InvalidProposalStatusTransitionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidProposalStatusTransition(
			InvalidProposalStatusTransitionException exception
	) {
		return ResponseEntity
				.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.of("INVALID_PROPOSAL_STATUS_TRANSITION", exception.getMessage()));
	}

	@ExceptionHandler(AiInsightsUnavailableException.class)
	public ResponseEntity<ErrorResponse> handleAiInsightsUnavailable(AiInsightsUnavailableException exception) {
		return ResponseEntity
				.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(ErrorResponse.of("AI_INSIGHTS_UNAVAILABLE", exception.getMessage()));
	}

	@ExceptionHandler(AiClientException.class)
	public ResponseEntity<ErrorResponse> handleAiClient(AiClientException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_GATEWAY)
				.body(ErrorResponse.of("AI_PROVIDER_ERROR", exception.getMessage()));
	}

	@ExceptionHandler(AiResponseParsingException.class)
	public ResponseEntity<ErrorResponse> handleAiResponseParsing(AiResponseParsingException exception) {
		return ResponseEntity
				.status(HttpStatus.BAD_GATEWAY)
				.body(ErrorResponse.of("AI_INVALID_RESPONSE", exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
				.getFieldErrors()
				.stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining("; "));

		return ResponseEntity
				.badRequest()
				.body(ErrorResponse.of("VALIDATION_ERROR", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception) {
		return ResponseEntity
				.badRequest()
				.body(ErrorResponse.of("MALFORMED_REQUEST", "Request body is invalid or has unsupported values"));
	}

	private String formatFieldError(FieldError fieldError) {
		return fieldError.getField() + ": " + fieldError.getDefaultMessage();
	}
}
