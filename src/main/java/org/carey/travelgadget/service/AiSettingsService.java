package org.carey.travelgadget.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.config.SettingsProperties;
import org.carey.travelgadget.config.ZhipuApiKeySupport;
import org.carey.travelgadget.domain.dto.ApiKeysStatusDto;
import org.carey.travelgadget.domain.dto.ApiKeysUpdateRequest;
import org.carey.travelgadget.domain.model.StoredApiKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSettingsService {

    private final SettingsProperties settingsProperties;
    private final ObjectMapper objectMapper;
    private final RagService ragService;

    @Value("${travelgadget.deepseek.api-key:}")
    private String yamlDeepseekKey;

    @Value("${spring.ai.dashscope.api-key:}")
    private String yamlDashscopeKey;

    @Value("${zhipu.api-key:}")
    private String yamlZhipuKey;

    private final StoredApiKeys stored = new StoredApiKeys();

    @PostConstruct
    public void loadFromFile() {
        Path path = Path.of(settingsProperties.getFile());
        if (!Files.exists(path)) {
            return;
        }
        try {
            String json = Files.readString(path);
            StoredApiKeys loaded = objectMapper.readValue(json, StoredApiKeys.class);
            mergeStored(loaded);
            log.info("已从 {} 加载 API Key 配置", path.toAbsolutePath());
        } catch (IOException e) {
            log.warn("读取 API 配置文件失败: {}", e.getMessage());
        }
    }

    public synchronized void updateKeys(ApiKeysUpdateRequest request) throws IOException {
        if (StringUtils.hasText(request.getDeepseekApiKey())) {
            stored.setDeepseekApiKey(request.getDeepseekApiKey().trim());
        }
        if (StringUtils.hasText(request.getDashscopeApiKey())) {
            stored.setDashscopeApiKey(request.getDashscopeApiKey().trim());
        }
        if (StringUtils.hasText(request.getZhipuApiKey())) {
            stored.setZhipuApiKey(request.getZhipuApiKey().trim());
        }
        persist();
    }

    public String resolveDeepseekKey() {
        return firstNonBlank(
                stored.getDeepseekApiKey(),
                yamlDeepseekKey,
                System.getenv("DEEPSEEK_API_KEY")
        );
    }

    public String resolveDashscopeKey() {
        return firstNonBlank(
                stored.getDashscopeApiKey(),
                yamlDashscopeKey,
                System.getenv("DASHSCOPE_API_KEY")
        );
    }

    public String resolveZhipuKey() {
        String key = firstNonBlank(
                stored.getZhipuApiKey(),
                yamlZhipuKey,
                System.getenv("ZHIPU_API_KEY"),
                System.getenv("ZAI_API_KEY")
        );
        return ZhipuApiKeySupport.isValid(key) ? key : null;
    }

    public boolean isDeepseekConfigured() {
        String key = resolveDeepseekKey();
        return StringUtils.hasText(key) && !key.startsWith("your-");
    }

    public boolean isDashscopeConfigured() {
        String key = resolveDashscopeKey();
        return StringUtils.hasText(key) && !key.startsWith("your-");
    }

    public boolean isZhipuConfigured() {
        return resolveZhipuKey() != null;
    }

    public ApiKeysStatusDto getStatus() {
        return ApiKeysStatusDto.builder()
                .deepseekConfigured(isDeepseekConfigured())
                .deepseekMasked(mask(resolveDeepseekKey()))
                .dashscopeConfigured(isDashscopeConfigured())
                .dashscopeMasked(mask(resolveDashscopeKey()))
                .zhipuConfigured(isZhipuConfigured())
                .zhipuMasked(mask(resolveZhipuKey()))
                .ragEnabled(isDashscopeConfigured())
                .ragKnowledgeLoaded(ragService.isKnowledgeLoaded())
                .settingsFile(Path.of(settingsProperties.getFile()).toAbsolutePath().toString())
                .note("默认读取 application.yml；本页保存会写入 data/api-settings.json 并覆盖。DeepSeek、智谱可热更新；DashScope 修改后需重启。")
                .build();
    }

    private void persist() throws IOException {
        Path path = Path.of(settingsProperties.getFile());
        Files.createDirectories(path.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), stored);
        log.info("API Key 已保存到 {}", path.toAbsolutePath());
    }

    private void mergeStored(StoredApiKeys loaded) {
        if (StringUtils.hasText(loaded.getDeepseekApiKey())) {
            stored.setDeepseekApiKey(loaded.getDeepseekApiKey());
        }
        if (StringUtils.hasText(loaded.getDashscopeApiKey())) {
            stored.setDashscopeApiKey(loaded.getDashscopeApiKey());
        }
        if (StringUtils.hasText(loaded.getZhipuApiKey())) {
            stored.setZhipuApiKey(loaded.getZhipuApiKey());
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return "";
    }

    public static String mask(String key) {
        if (!StringUtils.hasText(key)) {
            return "未配置";
        }
        String trimmed = key.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 4) + "****" + trimmed.substring(trimmed.length() - 4);
    }
}
