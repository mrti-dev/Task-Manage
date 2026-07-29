package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.WorkspaceRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WorkspaceMemberCreateRequest {
	@NotNull
	private Long workspaceId;

	@NotBlank
	@Email
	private String email;

	@NotNull
	private WorkspaceRole role;
}
