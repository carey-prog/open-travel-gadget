package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.config.DeepSeekProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DynamicChatClientFactory {

    private final AiSettingsService aiSettingsService;
    private final DeepSeekProperties deepSeekProperties;

    private volatile ChatClient cachedClient;
    private volatile String cachedKey;

    public ChatClient getClient() {
        String apiKey = aiSettingsService.resolveDeepseekKey();
        if (!StringUtils.hasText(apiKey) || apiKey.startsWith("your-")) {
            throw new IllegalStateException("未配置 DeepSeek API Key，请前往「API 配置」页面填写");
        }
        if (cachedClient != null && apiKey.equals(cachedKey)) {
            return cachedClient;
        }
        synchronized (this) {
            if (cachedClient != null && apiKey.equals(cachedKey)) {
                return cachedClient;
            }
            cachedClient = buildClient(apiKey);
            cachedKey = apiKey;
            return cachedClient;
        }
    }

    public void invalidate() {
        cachedClient = null;
        cachedKey = null;
    }

    private ChatClient buildClient(String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        OpenAiApi openAiApi = OpenAiApi.builder()
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .baseUrl(deepSeekProperties.getBaseUrl())
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(deepSeekProperties.getModel())
                .temperature(deepSeekProperties.getTemperature())
                .build();
        ChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(model)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
