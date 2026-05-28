package org.carey.travelgadget;

import org.carey.travelgadget.config.SettingsProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("org.carey.travelgadget.mapper")
@EnableConfigurationProperties({SettingsProperties.class})
public class TravelGadgetApplication {

    public static void main(String[] args) {
        SpringApplication.run(TravelGadgetApplication.class, args);
    }
}
