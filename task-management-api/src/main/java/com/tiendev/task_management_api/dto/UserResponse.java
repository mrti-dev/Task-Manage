package com.tiendev.task_management_api.dto;

import java.time.LocalDateTime;

import com.tiendev.task_management_api.model.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
	private Long id;
	private String username;
	private String email;
	private String avatar;
	private Role role;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
