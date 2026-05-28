package org.carey.travelgadget.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.service.ZhipuClientHolder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
@Configuration
@EnableConfigurationProperties(ZhipuProperties.class)
@RequiredArgsConstructor
public class ZhipuConfig {

    private final ZhipuClientHolder zhipuClientHolder;

    @EventListener(ApplicationReadyEvent.class)
    public void initZhipuClient() {
        if (zhipuClientHolder.refreshClient() == null) {
            log.warn("智谱 WebSearch 未配置，可在页面「API 配置」填写 zhipu.api-key（格式 id.secret）");
        }
    }
}
