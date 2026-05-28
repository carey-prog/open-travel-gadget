package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.TripGenerateRequest;
import org.carey.travelgadget.domain.model.DepartureCity;
import org.carey.travelgadget.domain.model.Destination;
import org.carey.travelgadget.domain.model.TransportPreference;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TripWorkflowService {

    private final AgentStateService agentStateService;
    private final TripGenerateRunner tripGenerateRunner;
    private final CityCatalogService cityCatalogService;

    public String startGenerate(TripGenerateRequest request) {
        Destination destination = cityCatalogService.requireDestination(request.getDestinationId());
        DepartureCity departure = cityCatalogService.requireDepartureCity(request.getDepartureCityId());

        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : agentStateService.createSessionId();

        Map<String, Object> initialState = new HashMap<>();
        initialState.put(TripGraphStateKeys.SESSION_ID, sessionId);
        initialState.put(TripGraphStateKeys.DESTINATION_ID, destination.getId());
        initialState.put(TripGraphStateKeys.DESTINATION_NAME, destination.getName());
        initialState.put(TripGraphStateKeys.DEPARTURE_CITY_ID, departure.getId());
        initialState.put(TripGraphStateKeys.DEPARTURE_CITY_NAME, departure.getName());
        initialState.put(TripGraphStateKeys.DAYS, request.getDays());
        initialState.put(TripGraphStateKeys.TRAVELERS, request.getTravelers());
        initialState.put(TripGraphStateKeys.BUDGET_TIER, request.getBudgetTier());
        initialState.put(TripGraphStateKeys.THEME, request.getTheme() != null ? request.getTheme() : "");
        initialState.put(TripGraphStateKeys.CUSTOM_REQUIRE, request.getCustomRequire() != null ? request.getCustomRequire() : "");
        initialState.put(TripGraphStateKeys.ARRIVAL_HUB, destination.getArrivalHub());
        initialState.put(TripGraphStateKeys.ARRIVAL_HUB_LABEL, destination.getArrivalHubLabel());
        initialState.put(TripGraphStateKeys.TRANSPORT_MODE, destination.getTransportMode());
        initialState.put(TripGraphStateKeys.CITIES, destination.getCities());
        initialState.put(TripGraphStateKeys.LOCAL_TRANSPORT_NOTE, destination.getLocalTransportNote());
        if (StringUtils.hasText(request.getDepartureDate())) {
            initialState.put(TripGraphStateKeys.DEPARTURE_DATE, request.getDepartureDate().trim());
        }
        String prefId = StringUtils.hasText(request.getTransportPreference())
                ? request.getTransportPreference().trim()
                : TransportPreference.HIGH_SPEED_RAIL.getId();
        initialState.put(TripGraphStateKeys.TRANSPORT_PREFERENCE, prefId);
        initialState.put(TripGraphStateKeys.TRANSPORT_PREFERENCE_LABEL, TransportPreference.labelOf(prefId));
        initialState.put(TripGraphStateKeys.STATUS, "running");

        agentStateService.saveState(sessionId, initialState);
        tripGenerateRunner.run(sessionId, initialState);
        return sessionId;
    }

    public Map<String, Object> getSessionState(String sessionId) {
        return agentStateService.getState(sessionId).orElse(Map.of());
    }
}
