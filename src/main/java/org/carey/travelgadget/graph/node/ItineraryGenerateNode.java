package org.carey.travelgadget.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.carey.travelgadget.service.AgentStateService;
import org.carey.travelgadget.service.LlmTripService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ItineraryGenerateNode implements NodeAction {

    private final LlmTripService llmTripService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        try {
            int days = state.value(TripGraphStateKeys.DAYS, Integer.class).orElse(3);
            String travelers = state.value(TripGraphStateKeys.TRAVELERS, String.class).orElse("");
            String budgetTier = state.value(TripGraphStateKeys.BUDGET_TIER, String.class).orElse("舒适");
            String theme = state.value(TripGraphStateKeys.THEME, String.class).orElse("");
            String customRequire = state.value(TripGraphStateKeys.CUSTOM_REQUIRE, String.class).orElse("");
            String departureCityName = state.value(TripGraphStateKeys.DEPARTURE_CITY_NAME, String.class).orElse("");
            String destinationName = state.value(TripGraphStateKeys.DESTINATION_NAME, String.class).orElse("");
            String arrivalLabel = state.value(TripGraphStateKeys.ARRIVAL_HUB_LABEL, String.class).orElse("");
            String cities = state.value(TripGraphStateKeys.CITIES, String.class).orElse("");
            String localNote = state.value(TripGraphStateKeys.LOCAL_TRANSPORT_NOTE, String.class).orElse("");
            String departureDate = state.value(TripGraphStateKeys.DEPARTURE_DATE, String.class).orElse("");
            String transportPrefLabel = state.value(TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL, String.class).orElse("");
            String ragContext = state.value(TripGraphStateKeys.RAG_CONTEXT, String.class).orElse("");
            String webContext = state.value(TripGraphStateKeys.WEB_CONTEXT, String.class).orElse("");
            String sessionId = state.value(TripGraphStateKeys.SESSION_ID, String.class).orElse("");

            GeneratedItineraryPayload payload = llmTripService.generateItinerary(
                    days, travelers, budgetTier, theme, customRequire,
                    departureCityName, destinationName, arrivalLabel, cities, localNote,
                    departureDate, transportPrefLabel, ragContext, webContext);

            String itineraryJson = objectMapper.writeValueAsString(payload);
            String budgetJson = objectMapper.writeValueAsString(payload.getBudget());

            agentStateService.mergeState(sessionId, Map.of(
                    TripGraphStateKeys.ITINERARY_JSON, itineraryJson,
                    TripGraphStateKeys.BUDGET_JSON, budgetJson,
                    TripGraphStateKeys.STATUS, "itinerary_generated"
            ));
            return Map.of(
                    TripGraphStateKeys.ITINERARY_JSON, itineraryJson,
                    TripGraphStateKeys.BUDGET_JSON, budgetJson
            );
        } catch (Exception e) {
            throw new IllegalStateException("行程生成失败", e);
        }
    }
}
