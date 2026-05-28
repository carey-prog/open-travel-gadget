package org.carey.travelgadget.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripGenerateRequest {

    private String sessionId;

    @NotBlank(message = "请选择目的地")
    private String destinationId;

    @NotBlank(message = "请选择出发地")
    private String departureCityId;

    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数至少为1天")
    @Max(value = 7, message = "天数最多为7天")
    private Integer days;

    @NotBlank(message = "同行人类型不能为空")
    private String travelers;

    @NotBlank(message = "预算档不能为空")
    private String budgetTier;

    private String theme;

    /** 计划出发日，格式 yyyy-MM-dd，可选 */
    private String departureDate;

    /** 大交通偏好：high_speed_rail / flight / train / cost_effective / less_transfer */
    private String transportPreference;

    private String customRequire;
}
