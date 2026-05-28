package org.carey.travelgadget.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "travelgadget.settings")
public class SettingsProperties {
    private String file = "./data/api-settings.json";
}
