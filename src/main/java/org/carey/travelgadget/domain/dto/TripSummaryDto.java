package org.carey.travelgadget.domain.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TripSummaryDto {
    private Long id;
    private String title;
    private Integer days;
    private String travelers;
    private String budgetTier;
    private String theme;
    private String departureCity;
    private String destinationName;
    private String summary;
    private LocalDateTime createdAt;
}
