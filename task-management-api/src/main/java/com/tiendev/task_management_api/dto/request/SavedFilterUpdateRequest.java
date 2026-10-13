package com.tiendev.task_management_api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilterUpdateRequest {

    @Size(max = 100, message = "Filter name must not exceed 100 characters")
    private String name;

    private String criteriaJson;
}
