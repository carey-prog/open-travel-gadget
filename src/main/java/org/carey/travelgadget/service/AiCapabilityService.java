package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.config.DeepSeekProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiCapabilityService {

    private final AiSettingsService aiSettingsService;
    private final DeepSeekProperties deepSeekProperties;
    private final CityCatalogService cityCatalogService;
    private final RagService ragService;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean embeddingEnabled = embeddingModelProvider.getIfAvailable() != null;
        boolean vectorReady = vectorStoreProvider.getIfAvailable() != null;

        status.put("deepseekEnabled", aiSettingsService.isDeepseekConfigured());
        status.put("deepseekModel", deepSeekProperties.getModel());
        status.put("zhipuEnabled", aiSettingsService.isZhipuConfigured());
        status.put("dashscopeConfigured", aiSettingsService.isDashscopeConfigured());
        status.put("embeddingEnabled", embeddingEnabled);
        status.put("ragEnabled", vectorReady && ragService.isKnowledgeLoaded());
        status.put("ragKnowledgeLoaded", ragService.isKnowledgeLoaded());
        status.put("destinationCount", cityCatalogService.getDestinationsById().size());
        status.put("departureCityCount", cityCatalogService.getDepartureCities().size());
        status.put("workflow", "RAG → 联网搜索 → DeepSeek 行程（含大交通建议）→ 保存");
        status.put("apiKeys", aiSettingsService.getStatus());
        return status;
    }
}
