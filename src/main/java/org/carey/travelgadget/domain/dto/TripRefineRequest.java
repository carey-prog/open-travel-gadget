package org.carey.travelgadget.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TripRefineRequest {

    @NotNull(message = "行程ID不能为空")
    private Long tripId;

    @NotBlank(message = "请描述要如何修改行程")
    private String message;
}
