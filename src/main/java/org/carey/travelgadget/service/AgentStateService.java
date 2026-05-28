package org.carey.travelgadget.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.config.AgentProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;

    public String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public void saveState(String sessionId, Map<String, Object> state) {
        String key = agentProperties.getSessionPrefix() + sessionId;
        redisTemplate.opsForValue().set(key, state, Duration.ofHours(agentProperties.getStateTtlHours()));
    }

    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getState(String sessionId) {
        String key = agentProperties.getSessionPrefix() + sessionId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Map<?, ?> map) {
            return Optional.of(objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}));
        }
        return Optional.empty();
    }

    public void mergeState(String sessionId, Map<String, Object> partial) {
        Map<String, Object> current = getState(sessionId).orElseGet(HashMap::new);
        current.putAll(partial);
        saveState(sessionId, current);
    }
}
