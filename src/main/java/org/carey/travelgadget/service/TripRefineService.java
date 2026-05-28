package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.travelgadget.domain.dto.ChatMessageDto;
import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.carey.travelgadget.domain.dto.TripRefineRequest;
import org.carey.travelgadget.graph.TripGraphStateKeys;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripRefineService {

    private final TripService tripService;
    private final LlmTripService llmTripService;
    private final RagService ragService;
    private final AgentStateService agentStateService;

    public TripDetailDto refine(TripRefineRequest request) {
        TripDetailDto current = tripService.getTripDetail(request.getTripId());
        if (current == null) {
            throw new IllegalStateException("行程不存在");
        }
        GeneratedItineraryPayload itinerary = current.getItinerary();
        if (itinerary == null) {
            throw new IllegalStateException("行程数据异常");
        }

        String destName = firstNonBlank(current.getDestinationName(), itinerary.getDestinationName(), "目的地");
        String depCity = firstNonBlank(current.getDepartureCity(), itinerary.getDepartureCity(), "");
        String datePart = current.getDepartureDate() != null ? current.getDepartureDate().toString() : "";
        String ragQuery = depCity + " " + destName + " " + datePart + " " + request.getMessage();
        String ragContext = ragService.search(ragQuery, 6, current.getDestinationId(), depCity);

        GeneratedItineraryPayload refined = llmTripService.refineItinerary(
                itinerary, current.getDays() != null ? current.getDays() : 3,
                destName, depCity, request.getMessage(), ragContext);

        TripDetailDto updated = tripService.updateItinerary(request.getTripId(), refined);

        if (current.getSessionId() != null && !current.getSessionId().isBlank()) {
            appendChatMessage(current.getSessionId(), request.getMessage(), refined.getSummary());
            agentStateService.mergeState(current.getSessionId(), Map.of(
                    TripGraphStateKeys.ITINERARY_JSON, tripService.toItineraryJson(refined),
                    TripGraphStateKeys.TRIP_ID, request.getTripId(),
                    TripGraphStateKeys.STATUS, "completed"
            ));
        }

        log.info("行程 {} 已根据用户反馈调整", request.getTripId());
        return updated;
    }

    @SuppressWarnings("unchecked")
    public List<ChatMessageDto> getChatHistory(String sessionId) {
        return agentStateService.getState(sessionId)
                .map(state -> {
                    Object raw = state.get("chatMessages");
                    if (raw instanceof List<?> list) {
                        List<ChatMessageDto> messages = new ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof Map<?, ?> map) {
                                messages.add(new ChatMessageDto(
                                        String.valueOf(map.get("role")),
                                        String.valueOf(map.get("content"))
                                ));
                            }
                        }
                        return messages;
                    }
                    return List.<ChatMessageDto>of();
                })
                .orElse(List.of());
    }

    private void appendChatMessage(String sessionId, String userMessage, String assistantSummary) {
        List<ChatMessageDto> history = new ArrayList<>(getChatHistory(sessionId));
        history.add(new ChatMessageDto("user", userMessage));
        history.add(new ChatMessageDto("assistant", assistantSummary != null ? assistantSummary : "已更新行程"));
        agentStateService.mergeState(sessionId, Map.of("chatMessages", history));
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return "";
    }
}
