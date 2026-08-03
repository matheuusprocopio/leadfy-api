package com.leadfy.api.exception;

public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String resourceName, Long id) {
		super(resourceName + " not found: " + id);
	}
}
