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
public class SavedFilterResponse {
    private Long id;
    private String name;
    private String criteriaJson;
    private Long workspaceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
