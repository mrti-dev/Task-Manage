package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.GlobalSearchResponse;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> search(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Long workspaceId) {
        GlobalSearchResponse response = searchService.search(q, workspaceId);
        return ApiResponse.success(response);
    }
}
