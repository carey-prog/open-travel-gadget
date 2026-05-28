package org.carey.travelgadget.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum TransportPreference {

    HIGH_SPEED_RAIL("high_speed_rail", "优先高铁"),
    FLIGHT("flight", "优先飞机"),
    TRAIN("train", "普速火车可接受"),
    COST_EFFECTIVE("cost_effective", "性价比优先"),
    LESS_TRANSFER("less_transfer", "少换乘直达");

    private final String id;
    private final String label;

    public static Optional<TransportPreference> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(p -> p.id.equals(id.trim()))
                .findFirst();
    }

    public static String labelOf(String id) {
        return fromId(id).map(TransportPreference::getLabel).orElse(id != null ? id : "未指定");
    }
}
