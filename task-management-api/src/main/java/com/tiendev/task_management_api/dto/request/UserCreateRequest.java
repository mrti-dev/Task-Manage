package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UserCreateRequest {
	@NotBlank
	@Size(min = 3, max = 50)
	private String username;

	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 8)
	private String password;

	private String avatar;

	private Role role;
}
