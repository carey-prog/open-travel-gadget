package org.carey.travelgadget.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Slf4j
@Configuration
public class RagConfig {

    @Bean
    public JedisPooled jedisPooled(RedisProperties redisProperties) {
        return new JedisPooled(new HostAndPort(redisProperties.getHost(), redisProperties.getPort()));
    }

    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore vectorStore(JedisPooled jedisPooled,
                                   EmbeddingModel embeddingModel,
                                   @Value("${spring.ai.vectorstore.redis.index-name:travel-gadget-rag}") String indexName,
                                   @Value("${spring.ai.vectorstore.redis.prefix:travel-rag:}") String prefix) {
        log.info("初始化 Travel Gadget Redis VectorStore，index={}, prefix={}", indexName, prefix);
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(true)
                .build();
    }
}
