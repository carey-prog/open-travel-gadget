package org.carey.travelgadget.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.carey.travelgadget.service.AgentStateService;
import org.carey.travelgadget.service.RagService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchNode implements NodeAction {

    private final RagService ragService;
    private final AgentStateService agentStateService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String sessionId = state.value(TripGraphStateKeys.SESSION_ID, String.class).orElse("");
        String ragContext = "";
        try {
            int days = state.value(TripGraphStateKeys.DAYS, Integer.class).orElse(3);
            String theme = state.value(TripGraphStateKeys.THEME, String.class).orElse("综合");
            String destinationName = state.value(TripGraphStateKeys.DESTINATION_NAME, String.class).orElse("");
            String destinationId = state.value(TripGraphStateKeys.DESTINATION_ID, String.class).orElse("");
            String departureCityName = state.value(TripGraphStateKeys.DEPARTURE_CITY_NAME, String.class).orElse("");
            String departureDate = state.value(TripGraphStateKeys.DEPARTURE_DATE, String.class).orElse("");
            String transportPref = state.value(TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL, String.class).orElse("");

            String query = compactRagQuery(departureCityName, destinationName, departureDate, transportPref, days, theme);
            log.info("RAG 节点检索，destination={}, query={}", destinationId, query);

            ragContext = ragService.search(query, 6, destinationId, null);
        } catch (Exception e) {
            log.warn("RAG 节点异常，已跳过: {}", e.getMessage());
        }

        agentStateService.mergeState(sessionId, Map.of(
                TripGraphStateKeys.RAG_CONTEXT, ragContext,
                TripGraphStateKeys.STATUS, "rag_done"
        ));
        return Map.of(TripGraphStateKeys.RAG_CONTEXT, ragContext);
    }

    private String compactRagQuery(String from, String to, String date, String transport, int days, String theme) {
        StringBuilder q = new StringBuilder();
        if (from != null && !from.isBlank()) {
            q.append(from.trim()).append("到");
        }
        if (to != null && !to.isBlank()) {
            q.append(to.trim());
        }
        if (date != null && !date.isBlank()) {
            q.append(" ").append(date.trim());
        }
        q.append(" ").append(days).append("天");
        if (transport != null && !transport.isBlank()) {
            q.append(" ").append(transport.trim());
        }
        if (theme != null && !theme.isBlank()) {
            q.append(" ").append(theme.trim());
        }
        q.append(" 行程攻略 交通");
        return q.toString().trim();
    }
}
