package org.carey.travelgadget.domain.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TripDetailDto {
    private Long id;
    private String sessionId;
    private String title;
    private Integer days;
    private String travelers;
    private String budgetTier;
    private String theme;
    private LocalDate departureDate;
    private String transportPreference;
    private String transportPreferenceLabel;
    private String departureCity;
    private String destinationId;
    private String destinationName;
    private String arrivalHub;
    private String arrivalHubLabel;
    private String transportMode;
    private String customRequire;
    private String summary;
    private GeneratedItineraryPayload itinerary;
    private Map<String, Object> budget;
    private LocalDateTime createdAt;
}
