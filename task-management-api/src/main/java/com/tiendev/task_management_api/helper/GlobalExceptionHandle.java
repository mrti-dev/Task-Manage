package com.tiendev.task_management_api.helper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandle {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
		return ApiResponse.error(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidOperationException.class)
	public ResponseEntity<?> handleInvalidOperation(InvalidOperationException ex) {
		return ApiResponse.error(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<?> handleBusinessException(BusinessException ex) {
		return ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<?> handleAlreadyExists(ResourceAlreadyExistsException ex) {
		return ApiResponse.error(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleAllException(Exception ex) {
		System.out.println(ex);
		return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
	}

	// loi truyen url
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String errorMessage = String.format("The parameter '%s' has a value '%s' that is incorrectly formatted.",
				ex.getName(), ex.getValue());
		return ApiResponse.error(HttpStatus.BAD_REQUEST, errorMessage);
	}

	// validate input
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
		List<String> errorList = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.toList());
		String errors = String.join("; ", errorList);

		ApiResponse<Object> response = new ApiResponse<>(HttpStatus.BAD_REQUEST, errors, null, "VALIDATION_ERROR");
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}
}
