package org.carey.travelgadget.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.carey.travelgadget.domain.model.StoredApiKeys;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring 自动配置之前注入 API Key：若存在 data/api-settings.json，则覆盖 application.yml 中的同名项。
 */
public class ApiSettingsEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE = "travelGadgetApiSettings";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String settingsFile = environment.getProperty("travelgadget.settings.file", "./data/api-settings.json");
        StoredApiKeys keys = loadFromJsonFile(Path.of(settingsFile));
        Map<String, Object> props = toPropertyMap(keys);
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE, props));
        }
    }

    private StoredApiKeys loadFromJsonFile(Path path) {
        StoredApiKeys keys = new StoredApiKeys();
        if (!Files.exists(path)) {
            return keys;
        }
        try {
            StoredApiKeys loaded = new ObjectMapper().readValue(path.toFile(), StoredApiKeys.class);
            if (StringUtils.hasText(loaded.getDeepseekApiKey())) {
                keys.setDeepseekApiKey(loaded.getDeepseekApiKey().trim());
            }
            if (StringUtils.hasText(loaded.getDashscopeApiKey())) {
                keys.setDashscopeApiKey(loaded.getDashscopeApiKey().trim());
            }
            if (StringUtils.hasText(loaded.getZhipuApiKey())) {
                keys.setZhipuApiKey(loaded.getZhipuApiKey().trim());
            }
        } catch (Exception ignored) {
        }
        return keys;
    }

    private Map<String, Object> toPropertyMap(StoredApiKeys keys) {
        Map<String, Object> props = new HashMap<>();
        if (StringUtils.hasText(keys.getDashscopeApiKey())) {
            props.put("spring.ai.dashscope.api-key", keys.getDashscopeApiKey());
        }
        if (StringUtils.hasText(keys.getDeepseekApiKey())) {
            props.put("travelgadget.deepseek.api-key", keys.getDeepseekApiKey());
        }
        if (StringUtils.hasText(keys.getZhipuApiKey())) {
            props.put("zhipu.api-key", keys.getZhipuApiKey());
        }
        return props;
    }
}
