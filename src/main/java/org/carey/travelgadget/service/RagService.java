package org.carey.travelgadget.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final ObjectProvider<JedisPooled> jedisProvider;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @Value("${spring.ai.vectorstore.redis.index-name:travel-gadget-rag}")
    private String indexName;

    @Value("${spring.ai.vectorstore.redis.prefix:travel-rag:}")
    private String keyPrefix;

    private VectorStore vectorStore;
    private volatile boolean knowledgeLoaded;
    private volatile int totalChunks;
    private volatile int loadedFileCount;
    private volatile Instant lastLoadedAt;
    private final List<String> loadedFiles = new ArrayList<>();

    public RagService(ObjectProvider<VectorStore> vectorStoreProvider,
                      ObjectProvider<JedisPooled> jedisProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
        this.jedisProvider = jedisProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(15)
    public void initKnowledgeBase() {
        if (knowledgeLoaded) {
            return;
        }
        synchronized (this) {
            if (!knowledgeLoaded) {
                loadKnowledgeBase(false);
            }
        }
    }

    public synchronized Map<String, Object> rebuildKnowledgeBase() {
        log.info("开始重建 RAG 知识库 index={}", indexName);
        dropVectorIndex();
        knowledgeLoaded = false;
        totalChunks = 0;
        loadedFileCount = 0;
        loadedFiles.clear();
        loadKnowledgeBase(true);
        return getStatus();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ready", vectorStore != null && knowledgeLoaded);
        status.put("vectorStoreAvailable", vectorStore != null);
        status.put("knowledgeLoaded", knowledgeLoaded);
        status.put("totalChunks", totalChunks);
        status.put("loadedFileCount", loadedFileCount);
        status.put("loadedFiles", List.copyOf(loadedFiles));
        status.put("indexName", indexName);
        status.put("keyPrefix", keyPrefix);
        status.put("lastLoadedAt", lastLoadedAt != null ? lastLoadedAt.toString() : null);
        status.put("hint", "修改 rag/cities/*.md 或 cities.yml 后，可调用 POST /api/system/rag/rebuild 重建索引（无需重启）");
        return status;
    }

    public boolean isKnowledgeLoaded() {
        return knowledgeLoaded;
    }

    public String search(String query, int topK) {
        return search(query, topK, null, null);
    }

    private static final int MAX_EMBED_QUERY_CHARS = 180;
    private static final int SEARCH_MAX_ATTEMPTS = 3;

    public String search(String query, int topK, String destinationId, String departureCityName) {
        if (vectorStore == null) {
            return "";
        }
        String embedQuery = truncateForEmbedding(buildSearchQuery(query, destinationId, departureCityName));
        int fetchK = Math.min(topK + 4, 12);

        for (int attempt = 1; attempt <= SEARCH_MAX_ATTEMPTS; attempt++) {
            try {
                List<Document> results = vectorStore.similaritySearch(
                        SearchRequest.builder().query(embedQuery).topK(fetchK).build());
                List<Document> ranked = rankByDestination(results, destinationId, topK);
                if (ranked.isEmpty()) {
                    return "";
                }
                return ranked.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));
            } catch (Exception e) {
                boolean transientNet = isTransientNetworkError(e);
                if (attempt < SEARCH_MAX_ATTEMPTS && transientNet) {
                    long waitMs = 800L * attempt;
                    log.warn("RAG 检索第 {} 次失败（{}），{}ms 后重试。query={}",
                            attempt, rootMessage(e), waitMs, embedQuery);
                    sleepQuietly(waitMs);
                    continue;
                }
                log.warn("RAG 检索不可用，已跳过本地知识库（行程将继续，仅依赖联网+大模型）。原因: {}",
                        rootMessage(e));
                return "";
            }
        }
        return "";
    }

    private void loadKnowledgeBase(boolean force) {
        this.vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未就绪，RAG 已禁用。请配置 DashScope Key 与 Redis Stack");
            return;
        }
        try {
            List<Resource> resources = discoverKnowledgeResources();
            TextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);
            int chunks = 0;
            loadedFiles.clear();
            for (Resource resource : resources) {
                String destinationId = resolveDestinationId(resource);
                String filename = resource.getFilename();
                TextReader reader = new TextReader(resource);
                reader.getCustomMetadata().put("source", filename);
                reader.getCustomMetadata().put("destination", destinationId);
                List<Document> documents = reader.get();
                for (Document doc : documents) {
                    doc.getMetadata().put("destination", destinationId);
                    doc.getMetadata().put("source", filename);
                }
                List<Document> docChunks = splitter.apply(documents);
                vectorStore.add(docChunks);
                chunks += docChunks.size();
                loadedFiles.add(filename + " → " + destinationId);
                log.info("RAG 已加载 {} (destination={})，{} 片段", filename, destinationId, docChunks.size());
            }
            totalChunks = chunks;
            loadedFileCount = resources.size();
            knowledgeLoaded = chunks > 0;
            lastLoadedAt = Instant.now();
            log.info("全国旅游 RAG 知识库共加载 {} 个片段，{} 个文件", chunks, resources.size());
        } catch (Exception e) {
            log.error("RAG 知识库加载失败: {}", e.getMessage(), e);
            knowledgeLoaded = false;
        }
    }

    private void dropVectorIndex() {
        JedisPooled jedis = jedisProvider.getIfAvailable();
        if (jedis == null) {
            return;
        }
        try {
            jedis.ftDropIndex(indexName);
            log.info("已删除 RediSearch 索引: {}", indexName);
        } catch (Exception e) {
            log.debug("删除索引 {} 时: {}", indexName, e.getMessage());
        }
        AtomicInteger deleted = new AtomicInteger();
        String cursor = ScanParams.SCAN_POINTER_START;
        ScanParams params = new ScanParams().match(keyPrefix + "*").count(200);
        do {
            ScanResult<String> scan = jedis.scan(cursor, params);
            List<String> keys = scan.getResult();
            if (!keys.isEmpty()) {
                jedis.del(keys.toArray(new String[0]));
                deleted.addAndGet(keys.size());
            }
            cursor = scan.getCursor();
        } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        if (deleted.get() > 0) {
            log.info("已清理 RAG 向量 key {} 个，前缀 {}", deleted.get(), keyPrefix);
        }
    }

    private List<Resource> discoverKnowledgeResources() throws Exception {
        List<Resource> resources = new ArrayList<>();
        for (String pattern : List.of("classpath:rag/cities/*.md", "classpath:rag/national-transport-guide.md")) {
            Resource[] found = resourceResolver.getResources(pattern);
            for (Resource r : found) {
                if (!r.exists() || !r.isReadable()) {
                    continue;
                }
                String name = r.getFilename();
                if (name != null && name.endsWith("-knowledge.md")) {
                    continue;
                }
                resources.add(r);
            }
        }
        resources.sort(Comparator.comparing(r -> {
            try {
                return r.getURI().toString();
            } catch (Exception e) {
                return r.getFilename();
            }
        }));
        return resources;
    }

    private String resolveDestinationId(Resource resource) {
        String filename = resource.getFilename();
        if (filename == null) {
            return "general";
        }
        if (filename.contains("national-transport")) {
            return "_national";
        }
        if (filename.contains("chaoshan")) {
            return "chaoshan";
        }
        String path = resource.getDescription();
        if (path != null && path.contains("/cities/")) {
            return filename.replace(".md", "");
        }
        return filename.replace("-knowledge.md", "").replace(".md", "");
    }

    /**
     * 构造简短检索句（避免重复关键词与英文 destinationId，降低 Embedding 请求体积）。
     */
    private String buildSearchQuery(String query, String destinationId, String departureCityName) {
        if (StringUtils.hasText(query)) {
            return query.trim();
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(departureCityName)) {
            sb.append(departureCityName).append("到");
        }
        if (StringUtils.hasText(destinationId) && !"_national".equals(destinationId)) {
            sb.append(destinationId.replace('_', ' ')).append(" ");
        }
        sb.append("旅游攻略 交通");
        return sb.toString().trim();
    }

    private String truncateForEmbedding(String query) {
        if (!StringUtils.hasText(query)) {
            return "旅游攻略";
        }
        String trimmed = query.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= MAX_EMBED_QUERY_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_EMBED_QUERY_CHARS);
    }

    private boolean isTransientNetworkError(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String msg = cur.getMessage() != null ? cur.getMessage().toLowerCase() : "";
            if (cur instanceof java.net.SocketException
                    || msg.contains("connection reset")
                    || msg.contains("connection timed out")
                    || msg.contains("read timed out")
                    || msg.contains("i/o error")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : e.getClass().getSimpleName();
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private List<Document> rankByDestination(List<Document> results, String destinationId, int topK) {
        if (!StringUtils.hasText(destinationId)) {
            return results.stream().limit(topK).toList();
        }
        List<Document> destFirst = new ArrayList<>();
        List<Document> national = new ArrayList<>();
        List<Document> others = new ArrayList<>();
        for (Document doc : results) {
            String dest = String.valueOf(doc.getMetadata().getOrDefault("destination", ""));
            if (destinationId.equals(dest)) {
                destFirst.add(doc);
            } else if ("_national".equals(dest)) {
                national.add(doc);
            } else {
                others.add(doc);
            }
        }
        List<Document> merged = new ArrayList<>();
        merged.addAll(destFirst);
        merged.addAll(national);
        merged.addAll(others);
        return merged.stream().limit(topK).toList();
    }
}
