package com.tiendev.task_management_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private UserResponse owner;
    private int memberCount;
    private int projectCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
