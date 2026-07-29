package com.tiendev.task_management_api.exception;

public class ResourceNotFoundException extends RuntimeException {
	private static final long serivalversionUID = 1L;

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
