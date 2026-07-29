package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Size(min = 3, max = 50)
    private String username;

    @Email
    private String email;

    @Size(min = 8)
    private String password;

    private String avatar;

    private Role role;
}
