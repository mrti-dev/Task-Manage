package com.tiendev.task_management_api.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
	private static final long serivalversionUID = 1L;

	public ResourceAlreadyExistsException(String message) {
		super(message);
	}
}
