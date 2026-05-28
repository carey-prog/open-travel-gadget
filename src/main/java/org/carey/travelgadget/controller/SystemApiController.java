package org.carey.travelgadget.controller;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.ApiResponse;
import org.carey.travelgadget.service.AiCapabilityService;
import org.carey.travelgadget.service.RagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemApiController {

    private final AiCapabilityService aiCapabilityService;
    private final RagService ragService;

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        Map<String, Object> status = new LinkedHashMap<>(aiCapabilityService.getStatus());
        status.put("rag", ragService.getStatus());
        return ApiResponse.ok(status);
    }

    @GetMapping("/rag/status")
    public ApiResponse<Map<String, Object>> ragStatus() {
        return ApiResponse.ok(ragService.getStatus());
    }

    @PostMapping("/rag/rebuild")
    public ApiResponse<Map<String, Object>> rebuildRag() {
        try {
            Map<String, Object> result = ragService.rebuildKnowledgeBase();
            return ApiResponse.ok("RAG 知识库已重建", result);
        } catch (Exception e) {
            return ApiResponse.fail("重建失败: " + e.getMessage());
        }
    }
}
