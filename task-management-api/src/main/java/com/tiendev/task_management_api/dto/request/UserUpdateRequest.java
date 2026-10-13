package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = ".*\\S.*", message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Email
    private String email;

    @Size(min = 8)
    private String password;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatar;

    private Role role;
}
