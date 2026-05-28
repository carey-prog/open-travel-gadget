package org.carey.travelgadget.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripGenerateRunner {

    private final CompiledGraph tripGraph;
    private final AgentStateService agentStateService;

    @Async("tripExecutor")
    public void run(String sessionId, Map<String, Object> initialState) {
        try {
            log.info("开始异步生成行程 sessionId={}", sessionId);
            Optional<OverAllState> result = tripGraph.invoke(initialState);
            OverAllState finalState = result.orElseThrow(() -> new IllegalStateException("工作流未返回结果"));
            agentStateService.saveState(sessionId, finalState.data());

            Long tripId = finalState.value(TripGraphStateKeys.TRIP_ID, Long.class).orElse(null);
            if (tripId == null) {
                String error = finalState.value(TripGraphStateKeys.ERROR, String.class).orElse("生成失败");
                throw new IllegalStateException(error);
            }
            log.info("行程生成完成 sessionId={}, tripId={}", sessionId, tripId);
        } catch (Exception e) {
            log.error("行程工作流失败 sessionId={}", sessionId, e);
            agentStateService.mergeState(sessionId, Map.of(
                    TripGraphStateKeys.STATUS, "failed",
                    TripGraphStateKeys.ERROR, e.getMessage() != null ? e.getMessage() : "未知错误"
            ));
        }
    }
}
