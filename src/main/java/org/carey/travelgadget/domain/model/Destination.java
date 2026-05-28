package org.carey.travelgadget.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class Destination {
    private String id;
    private String name;
    private String cities;
    private String arrivalHub;
    private String arrivalHubLabel;
    private String transportMode;
    private String localTransportNote;
    private List<String> themeOptions;
}
