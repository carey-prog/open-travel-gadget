package org.carey.travelgadget.controller;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.ApiResponse;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.carey.travelgadget.service.TripShareService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
public class ShareApiController {

    private final TripShareService tripShareService;

    @GetMapping("/{token}")
    public ApiResponse<TripDetailDto> getSharedTrip(@PathVariable String token) {
        TripDetailDto detail = tripShareService.getByShareToken(token);
        if (detail == null) {
            return ApiResponse.fail("分享链接无效或已失效");
        }
        return ApiResponse.ok(detail);
    }
}
