package org.carey.travelgadget.service;

import ai.z.openapi.ZhipuAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.config.ZhipuApiKeySupport;
import org.carey.travelgadget.config.ZhipuProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZhipuClientHolder {

    private final AiSettingsService aiSettingsService;
    private final ZhipuProperties zhipuProperties;

    private final AtomicReference<ZhipuAiClient> clientRef = new AtomicReference<>();

    public ZhipuAiClient getClient() {
        ZhipuAiClient existing = clientRef.get();
        if (existing != null) {
            return existing;
        }
        return refreshClient();
    }

    public ZhipuAiClient refreshClient() {
        String apiKey = aiSettingsService.resolveZhipuKey();
        if (apiKey == null) {
            clientRef.set(null);
            return null;
        }
        ZhipuAiClient.Builder builder = ZhipuAiClient.builder().ofZHIPU();
        if (StringUtils.hasText(zhipuProperties.baseUrl())) {
            builder.baseUrl(zhipuProperties.baseUrl());
        }
        builder.apiKey(ZhipuApiKeySupport.resolve(apiKey));
        ZhipuAiClient client = builder.build();
        clientRef.set(client);
        log.info("智谱 WebSearch 客户端已刷新");
        return client;
    }

    public void invalidate() {
        clientRef.set(null);
    }
}
