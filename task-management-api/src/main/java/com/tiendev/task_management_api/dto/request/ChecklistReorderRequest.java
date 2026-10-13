package com.tiendev.task_management_api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistReorderRequest {

    @NotEmpty(message = "Item IDs list must not be empty")
    private List<Long> itemIds;
}
