package org.carey.travelgadget.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.carey.travelgadget.domain.dto.TripSummaryDto;
import org.carey.travelgadget.domain.entity.Trip;
import org.carey.travelgadget.mapper.TripMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripService {

    private static final int MAX_STORED_CONTEXT_CHARS = 16000;

    private final TripMapper tripMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long saveGeneratedTrip(String sessionId,
                                 int days,
                                 String travelers,
                                 String budgetTier,
                                 String theme,
                                 String customRequire,
                                 String departureCity,
                                 String destinationId,
                                 String destinationName,
                                 String arrivalHub,
                                 String arrivalHubLabel,
                                 String transportMode,
                                 String departureDate,
                                 String transportPreference,
                                 String transportPreferenceLabel,
                                 String ragContext,
                                 String webContext,
                                 GeneratedItineraryPayload payload,
                                 Map<String, Object> budget) {
        try {
            Trip trip = new Trip();
            trip.setSessionId(sessionId);
            trip.setTitle(payload.getTitle());
            trip.setDays(days);
            trip.setTravelers(travelers);
            trip.setBudgetTier(budgetTier);
            trip.setTheme(theme);
            trip.setDepartureDate(parseDate(departureDate));
            trip.setTransportPreference(transportPreference);
            trip.setTransportPreferenceLabel(transportPreferenceLabel);
            trip.setDepartureCity(firstNonBlank(payload.getDepartureCity(), departureCity));
            trip.setDestinationId(destinationId);
            trip.setDestinationName(firstNonBlank(payload.getDestinationName(), destinationName));
            trip.setArrivalHub(firstNonBlank(payload.getArrivalHub(), arrivalHub));
            trip.setArrivalHubLabel(arrivalHubLabel);
            trip.setTransportMode(transportMode);
            trip.setCustomRequire(customRequire);
            trip.setSummary(payload.getSummary());
            trip.setItineraryJson(objectMapper.writeValueAsString(payload));
            trip.setBudgetJson(objectMapper.writeValueAsString(budget));
            trip.setRagContext(limitContextLength(ragContext));
            trip.setWebContext(limitContextLength(webContext));
            trip.setStatus("completed");
            tripMapper.insert(trip);
            return trip.getId();
        } catch (Exception e) {
            throw new IllegalStateException("保存行程失败", e);
        }
    }

    @Transactional
    public TripDetailDto updateItinerary(Long id, GeneratedItineraryPayload payload) {
        try {
            Trip trip = tripMapper.selectById(id);
            if (trip == null) {
                throw new IllegalStateException("行程不存在");
            }
            trip.setTitle(payload.getTitle());
            trip.setSummary(payload.getSummary());
            if (StringUtils.hasText(payload.getDepartureCity())) {
                trip.setDepartureCity(payload.getDepartureCity());
            }
            if (StringUtils.hasText(payload.getDestinationName())) {
                trip.setDestinationName(payload.getDestinationName());
            }
            trip.setItineraryJson(objectMapper.writeValueAsString(payload));
            if (payload.getBudget() != null) {
                trip.setBudgetJson(objectMapper.writeValueAsString(payload.getBudget()));
            }
            tripMapper.updateById(trip);
            return getTripDetail(id);
        } catch (Exception e) {
            throw new IllegalStateException("更新行程失败", e);
        }
    }

    public String toItineraryJson(GeneratedItineraryPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return "{}";
        }
    }

    public List<TripSummaryDto> listRecentTrips(int limit) {
        LambdaQueryWrapper<Trip> wrapper = new LambdaQueryWrapper<Trip>()
                .orderByDesc(Trip::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50));
        return tripMapper.selectList(wrapper).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    private TripSummaryDto toSummary(Trip trip) {
        TripSummaryDto dto = new TripSummaryDto();
        dto.setId(trip.getId());
        dto.setTitle(trip.getTitle());
        dto.setDays(trip.getDays());
        dto.setTravelers(trip.getTravelers());
        dto.setBudgetTier(trip.getBudgetTier());
        dto.setTheme(trip.getTheme());
        dto.setDepartureCity(trip.getDepartureCity());
        dto.setDestinationName(trip.getDestinationName());
        dto.setSummary(trip.getSummary());
        dto.setCreatedAt(trip.getCreatedAt());
        return dto;
    }

    public TripDetailDto getTripDetail(Long id) {
        Trip trip = tripMapper.selectById(id);
        if (trip == null) {
            return null;
        }
        try {
            TripDetailDto dto = new TripDetailDto();
            dto.setId(trip.getId());
            dto.setSessionId(trip.getSessionId());
            dto.setTitle(trip.getTitle());
            dto.setDays(trip.getDays());
            dto.setTravelers(trip.getTravelers());
            dto.setBudgetTier(trip.getBudgetTier());
            dto.setTheme(trip.getTheme());
            dto.setDepartureDate(trip.getDepartureDate());
            dto.setTransportPreference(trip.getTransportPreference());
            dto.setTransportPreferenceLabel(trip.getTransportPreferenceLabel());
            dto.setDepartureCity(trip.getDepartureCity());
            dto.setDestinationId(trip.getDestinationId());
            dto.setDestinationName(trip.getDestinationName());
            dto.setArrivalHub(trip.getArrivalHub());
            dto.setArrivalHubLabel(trip.getArrivalHubLabel());
            dto.setTransportMode(trip.getTransportMode());
            dto.setCustomRequire(trip.getCustomRequire());
            dto.setSummary(trip.getSummary());
            dto.setItinerary(objectMapper.readValue(trip.getItineraryJson(), GeneratedItineraryPayload.class));
            if (trip.getBudgetJson() != null) {
                dto.setBudget(objectMapper.readValue(trip.getBudgetJson(), new TypeReference<>() {}));
            }
            dto.setCreatedAt(trip.getCreatedAt());
            return dto;
        } catch (Exception e) {
            throw new IllegalStateException("解析行程详情失败", e);
        }
    }

    private String limitContextLength(String context) {
        if (context == null || context.length() <= MAX_STORED_CONTEXT_CHARS) {
            return context;
        }
        return context.substring(0, MAX_STORED_CONTEXT_CHARS) + "\n...(truncated)";
    }

    private String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b;
    }

    private LocalDate parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
