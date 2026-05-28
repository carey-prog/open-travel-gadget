package org.carey.travelgadget.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trip")
public class Trip {

    @TableId(type = IdType.AUTO)
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
    private String itineraryJson;
    private String budgetJson;
    private String ragContext;
    private String webContext;
    private String status;
    private String shareToken;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
