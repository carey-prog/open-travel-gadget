package org.carey.travelgadget.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.ApiKeysStatusDto;
import org.carey.travelgadget.domain.dto.ApiKeysUpdateRequest;
import org.carey.travelgadget.domain.dto.ApiResponse;
import org.carey.travelgadget.service.AiCapabilityService;
import org.carey.travelgadget.service.AiSettingsService;
import org.carey.travelgadget.service.DynamicChatClientFactory;
import org.carey.travelgadget.service.ZhipuClientHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsApiController {

    private final AiSettingsService aiSettingsService;
    private final AiCapabilityService aiCapabilityService;
    private final DynamicChatClientFactory chatClientFactory;
    private final ZhipuClientHolder zhipuClientHolder;

    @GetMapping("/keys")
    public ApiResponse<ApiKeysStatusDto> getKeys() {
        return ApiResponse.ok(aiSettingsService.getStatus());
    }

    @PutMapping("/keys")
    public ApiResponse<ApiKeysStatusDto> updateKeys(@Valid @RequestBody ApiKeysUpdateRequest request) {
        try {
            aiSettingsService.updateKeys(request);
            chatClientFactory.invalidate();
            zhipuClientHolder.refreshClient();
            ApiKeysStatusDto status = aiSettingsService.getStatus();
            return ApiResponse.ok("配置已保存。DeepSeek 与智谱已热更新；若修改了 DashScope，请重启应用。", status);
        } catch (Exception e) {
            return ApiResponse.fail("保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.ok(aiCapabilityService.getStatus());
    }
}
