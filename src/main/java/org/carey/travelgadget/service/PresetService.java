package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.TripPresetDto;
import org.carey.travelgadget.domain.model.Destination;
import org.carey.travelgadget.domain.model.TransportPreference;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresetService {

    private final CityCatalogService cityCatalogService;

    public TripPresetDto getPresets() {
        List<TripPresetDto.DestinationOptionDto> destinations = cityCatalogService.getDestinationsById().values().stream()
                .map(this::toOption)
                .toList();

        List<TripPresetDto.TransportPreferenceOptionDto> transportPrefs = Arrays.stream(TransportPreference.values())
                .map(p -> TripPresetDto.TransportPreferenceOptionDto.builder()
                        .id(p.getId())
                        .label(p.getLabel())
                        .build())
                .toList();

        return TripPresetDto.builder()
                .destinations(destinations)
                .departureCities(cityCatalogService.getDepartureCities())
                .travelerOptions(List.of("独自", "情侣", "亲子", "朋友", "老人同行"))
                .budgetTiers(List.of("经济", "舒适"))
                .transportPreferences(transportPrefs)
                .defaultTransportPreference(TransportPreference.HIGH_SPEED_RAIL.getId())
                .defaultDestinationId("chaoshan")
                .defaultDepartureCityId("guangzhou")
                .build();
    }

    private TripPresetDto.DestinationOptionDto toOption(Destination d) {
        return TripPresetDto.DestinationOptionDto.builder()
                .id(d.getId())
                .name(d.getName())
                .cities(Arrays.asList(d.getCities().split(",")))
                .arrivalHubLabel(d.getArrivalHubLabel())
                .localTransportNote(d.getLocalTransportNote())
                .themeOptions(d.getThemeOptions())
                .build();
    }
}
