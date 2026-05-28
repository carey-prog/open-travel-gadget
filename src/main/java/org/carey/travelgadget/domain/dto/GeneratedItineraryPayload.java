package org.carey.travelgadget.domain.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GeneratedItineraryPayload {

    private String title;
    private String summary;
    private String departureCity;
    private String destinationName;
    private String arrivalHub;
    private String transportNote;
    private List<TransportSuggestion> transportSuggestions;
    private List<DayPlan> days;
    private List<String> foodRecommendations;
    private List<String> warnings;
    private Map<String, Object> budget;

    @Data
    public static class TransportSuggestion {
        /** 高铁 / 飞机 / 火车 */
        private String mode;
        private String route;
        private String duration;
        private String scheduleHint;
        private String priceHint;
        /** 推荐 / 备选 */
        private String priority;
        private String note;
    }

    @Data
    public static class DayPlan {
        private int dayIndex;
        private String city;
        private String theme;
        private List<TimeSlot> slots;
        private String transportBetween;
        private String accommodationArea;
    }

    @Data
    public static class TimeSlot {
        private String period;
        private String poiName;
        private String activity;
        private int durationMinutes;
        private String transport;
        private String tips;
    }
}
