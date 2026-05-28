package org.carey.travelgadget.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.ApiResponse;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.carey.travelgadget.domain.dto.TripGenerateRequest;
import org.carey.travelgadget.domain.dto.TripPresetDto;
import org.carey.travelgadget.domain.dto.TripRefineRequest;
import org.carey.travelgadget.domain.dto.TripSummaryDto;
import org.carey.travelgadget.domain.model.DepartureCity;
import org.carey.travelgadget.domain.model.Destination;
import org.carey.travelgadget.service.CityCatalogService;
import org.carey.travelgadget.service.PresetService;
import org.carey.travelgadget.service.TripExportService;
import org.carey.travelgadget.service.TripRefineService;
import org.carey.travelgadget.service.TripService;
import org.carey.travelgadget.service.TripShareService;
import org.carey.travelgadget.service.TripWorkflowService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TripApiController {

    private final TripWorkflowService tripWorkflowService;
    private final TripService tripService;
    private final TripRefineService tripRefineService;
    private final TripExportService tripExportService;
    private final TripShareService tripShareService;
    private final PresetService presetService;
    private final CityCatalogService cityCatalogService;

    @GetMapping("/trip/presets")
    public ApiResponse<TripPresetDto> presets() {
        return ApiResponse.ok(presetService.getPresets());
    }

    @PostMapping("/trip/generate")
    public ApiResponse<Map<String, Object>> generate(@Valid @RequestBody TripGenerateRequest request) {
        Destination destination = cityCatalogService.requireDestination(request.getDestinationId());
        DepartureCity departure = cityCatalogService.requireDepartureCity(request.getDepartureCityId());
        String sessionId = tripWorkflowService.startGenerate(request);
        return ApiResponse.ok("已开始生成行程", Map.of(
                "sessionId", sessionId,
                "status", "running",
                "destinationId", destination.getId(),
                "destinationName", destination.getName(),
                "departureCityId", departure.getId(),
                "departureCityName", departure.getName(),
                "arrivalHub", destination.getArrivalHub(),
                "arrivalHubLabel", destination.getArrivalHubLabel()
        ));
    }

    @GetMapping("/agent/session/{sessionId}")
    public ApiResponse<Map<String, Object>> sessionState(@PathVariable String sessionId) {
        return ApiResponse.ok(tripWorkflowService.getSessionState(sessionId));
    }

    @GetMapping("/trip/list")
    public ApiResponse<List<TripSummaryDto>> listTrips(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(tripService.listRecentTrips(limit));
    }

    @GetMapping("/trip/{id}")
    public ApiResponse<TripDetailDto> getTrip(@PathVariable Long id) {
        TripDetailDto detail = tripService.getTripDetail(id);
        if (detail == null) {
            return ApiResponse.fail("行程不存在");
        }
        return ApiResponse.ok(detail);
    }

    @GetMapping("/trip/{id}/export")
    public ResponseEntity<byte[]> exportTrip(@PathVariable Long id) {
        TripDetailDto detail = tripService.getTripDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        String markdown = tripExportService.toMarkdown(detail);
        String filename = "trip-" + id + ".md";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "markdown", StandardCharsets.UTF_8))
                .body(markdown.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/trip/{id}/share")
    public ApiResponse<Map<String, Object>> shareTrip(@PathVariable Long id) {
        try {
            Map<String, Object> result = new LinkedHashMap<>(tripShareService.createShareLink(id));
            result.put("shareUrl", result.get("sharePath"));
            return ApiResponse.ok("分享链接已生成", result);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/trip/refine")
    public ApiResponse<TripDetailDto> refineTrip(@Valid @RequestBody TripRefineRequest request) {
        try {
            TripDetailDto updated = tripRefineService.refine(request);
            return ApiResponse.ok("行程已更新", updated);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @GetMapping("/trip/session/{sessionId}/chat")
    public ApiResponse<?> chatHistory(@PathVariable String sessionId) {
        return ApiResponse.ok(tripRefineService.getChatHistory(sessionId));
    }
}
