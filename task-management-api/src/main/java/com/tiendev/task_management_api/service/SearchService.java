package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.GlobalSearchResponse;

public interface SearchService {

    GlobalSearchResponse search(String query, Long workspaceId);
}
