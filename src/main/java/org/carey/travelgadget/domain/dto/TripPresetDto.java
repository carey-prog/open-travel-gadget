package org.carey.travelgadget.domain.dto;

import lombok.Builder;
import lombok.Data;
import org.carey.travelgadget.domain.model.DepartureCity;
import org.carey.travelgadget.domain.model.Destination;

import java.util.List;

@Data
@Builder
public class TripPresetDto {
    private List<DestinationOptionDto> destinations;
    private List<DepartureCity> departureCities;
    private List<String> travelerOptions;
    private List<String> budgetTiers;
    private String defaultDestinationId;
    private String defaultDepartureCityId;
    private List<TransportPreferenceOptionDto> transportPreferences;
    private String defaultTransportPreference;

    @Data
    @Builder
    public static class TransportPreferenceOptionDto {
        private String id;
        private String label;
    }

    @Data
    @Builder
    public static class DestinationOptionDto {
        private String id;
        private String name;
        private List<String> cities;
        private String arrivalHubLabel;
        private String localTransportNote;
        private List<String> themeOptions;
    }
}
