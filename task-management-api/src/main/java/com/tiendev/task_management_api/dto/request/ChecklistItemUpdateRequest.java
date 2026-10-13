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
public class ChecklistItemUpdateRequest {

    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    private Boolean completed;
}
