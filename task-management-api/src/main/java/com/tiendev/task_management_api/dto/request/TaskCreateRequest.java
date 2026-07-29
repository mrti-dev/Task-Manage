package com.tiendev.task_management_api.dto.request;

import java.time.LocalDate;

import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateRequest {
	@NotNull
	private Long projectId;

	@NotBlank
	private String title;

	private String description;

	@NotNull
	private TaskPriority priority;

	private TaskStatus status;

	@Email
	private String assigneeEmail;

	private LocalDate deadline;
}
