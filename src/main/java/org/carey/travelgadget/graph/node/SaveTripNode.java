package org.carey.travelgadget.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.carey.travelgadget.service.AgentStateService;
import org.carey.travelgadget.service.TripService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SaveTripNode implements NodeAction {

    private final TripService tripService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        try {
            String sessionId = state.value(TripGraphStateKeys.SESSION_ID, String.class).orElse("");
            int days = state.value(TripGraphStateKeys.DAYS, Integer.class).orElse(3);
            String travelers = state.value(TripGraphStateKeys.TRAVELERS, String.class).orElse("");
            String budgetTier = state.value(TripGraphStateKeys.BUDGET_TIER, String.class).orElse("");
            String theme = state.value(TripGraphStateKeys.THEME, String.class).orElse("");
            String customRequire = state.value(TripGraphStateKeys.CUSTOM_REQUIRE, String.class).orElse("");
            String departureCityName = state.value(TripGraphStateKeys.DEPARTURE_CITY_NAME, String.class).orElse("");
            String destinationId = state.value(TripGraphStateKeys.DESTINATION_ID, String.class).orElse("");
            String destinationName = state.value(TripGraphStateKeys.DESTINATION_NAME, String.class).orElse("");
            String arrivalHub = state.value(TripGraphStateKeys.ARRIVAL_HUB, String.class).orElse("");
            String arrivalHubLabel = state.value(TripGraphStateKeys.ARRIVAL_HUB_LABEL, String.class).orElse("");
            String transportMode = state.value(TripGraphStateKeys.TRANSPORT_MODE, String.class).orElse("");
            String departureDate = state.value(TripGraphStateKeys.DEPARTURE_DATE, String.class).orElse(null);
            String transportPreference = state.value(TripGraphStateKeys.TRANSPORT_PREFERENCE, String.class).orElse(null);
            String transportPreferenceLabel = state.value(TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL, String.class).orElse(null);
            String ragContext = state.value(TripGraphStateKeys.RAG_CONTEXT, String.class).orElse("");
            String webContext = state.value(TripGraphStateKeys.WEB_CONTEXT, String.class).orElse("");
            String itineraryJson = state.value(TripGraphStateKeys.ITINERARY_JSON, String.class).orElse("{}");
            String budgetJson = state.value(TripGraphStateKeys.BUDGET_JSON, String.class).orElse("{}");

            GeneratedItineraryPayload payload = objectMapper.readValue(itineraryJson, GeneratedItineraryPayload.class);
            Map<String, Object> budget = objectMapper.readValue(budgetJson, new TypeReference<>() {});

            Long tripId = tripService.saveGeneratedTrip(
                    sessionId, days, travelers, budgetTier, theme, customRequire,
                    departureCityName, destinationId, destinationName,
                    arrivalHub, arrivalHubLabel, transportMode,
                    departureDate, transportPreference, transportPreferenceLabel,
                    ragContext, webContext, payload, budget);

            agentStateService.mergeState(sessionId, Map.of(
                    TripGraphStateKeys.TRIP_ID, tripId,
                    TripGraphStateKeys.STATUS, "completed"
            ));
            return Map.of(
                    TripGraphStateKeys.TRIP_ID, tripId,
                    TripGraphStateKeys.STATUS, "completed"
            );
        } catch (Exception e) {
            throw new IllegalStateException("保存行程失败", e);
        }
    }
}
