package org.carey.travelgadget.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@Conditional(DashScopeApiKeyPresentCondition.class)
public class DashScopeEmbeddingConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel dashScopeEmbeddingModel(
            @Value("${spring.ai.dashscope.api-key}") String apiKey,
            @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}") String model) {
        log.info("初始化 DashScope EmbeddingModel，model={}", model);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(factory);
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(model)
                .build();
        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, options);
    }
}
