package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final ZhipuWebSearchService zhipuWebSearchService;

    public String search(String query) {
        try {
            String result = zhipuWebSearchService.search(query);
            if (result != null && !result.isBlank()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("智谱联网搜索失败: {}", e.getMessage());
        }
        log.warn("智谱 WebSearch 未启用，使用降级提示");
        return buildFallbackContext(query);
    }

    private String buildFallbackContext(String query) {
        return "当前未接入联网搜索，请结合本地攻略库作答。搜索主题：" + query;
    }
}
