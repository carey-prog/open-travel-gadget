package org.carey.travelgadget.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.carey.travelgadget.service.AgentStateService;
import org.carey.travelgadget.service.WebSearchService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSearchNode implements NodeAction {

    private final WebSearchService webSearchService;
    private final AgentStateService agentStateService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        int days = state.value(TripGraphStateKeys.DAYS, Integer.class).orElse(3);
        String theme = state.value(TripGraphStateKeys.THEME, String.class).orElse("");
        String destinationName = state.value(TripGraphStateKeys.DESTINATION_NAME, String.class).orElse("");
        String departureCityName = state.value(TripGraphStateKeys.DEPARTURE_CITY_NAME, String.class).orElse("");
        String arrivalLabel = state.value(TripGraphStateKeys.ARRIVAL_HUB_LABEL, String.class).orElse("");
        String departureDate = state.value(TripGraphStateKeys.DEPARTURE_DATE, String.class).orElse("");
        String transportPref = state.value(TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL, String.class).orElse("");

        String query = departureCityName + " 到 " + destinationName + " " + departureDate + " "
                + transportPref + " " + days + "日游 " + theme
                + " 高铁 飞机 火车 班次 票价 " + arrivalLabel + " 景点开放时间 天气";
        String webContext = webSearchService.search(query);

        String sessionId = state.value(TripGraphStateKeys.SESSION_ID, String.class).orElse("");
        agentStateService.mergeState(sessionId, Map.of(
                TripGraphStateKeys.WEB_CONTEXT, webContext,
                TripGraphStateKeys.STATUS, "web_search_done"
        ));
        return Map.of(TripGraphStateKeys.WEB_CONTEXT, webContext);
    }
}
