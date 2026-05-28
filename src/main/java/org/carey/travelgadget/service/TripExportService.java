package org.carey.travelgadget.service;

import org.carey.travelgadget.domain.dto.GeneratedItineraryPayload;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class TripExportService {

    public String toMarkdown(TripDetailDto trip) {
        if (trip == null) {
            return "";
        }
        GeneratedItineraryPayload it = trip.getItinerary();
        StringBuilder md = new StringBuilder();
        md.append("# ").append(safe(it != null ? it.getTitle() : trip.getTitle())).append("\n\n");
        md.append("- **出发地**：").append(safe(trip.getDepartureCity())).append("\n");
        md.append("- **目的地**：").append(safe(trip.getDestinationName())).append("\n");
        if (trip.getDepartureDate() != null) {
            md.append("- **计划出发日**：").append(trip.getDepartureDate()).append("\n");
        }
        md.append("- **天数**：").append(trip.getDays()).append(" 天\n");
        md.append("- **同行人**：").append(safe(trip.getTravelers())).append("\n");
        md.append("- **预算档**：").append(safe(trip.getBudgetTier())).append("\n");
        if (StringUtils.hasText(trip.getTransportPreferenceLabel())) {
            md.append("- **交通偏好**：").append(trip.getTransportPreferenceLabel()).append("\n");
        }
        md.append("- **抵达枢纽**：").append(safe(trip.getArrivalHubLabel())).append("\n\n");

        if (it != null && StringUtils.hasText(it.getSummary())) {
            md.append("> ").append(it.getSummary()).append("\n\n");
        }
        if (it != null && StringUtils.hasText(it.getTransportNote())) {
            md.append(it.getTransportNote()).append("\n\n");
        }

        if (it != null && it.getTransportSuggestions() != null && !it.getTransportSuggestions().isEmpty()) {
            md.append("## 大交通建议\n\n");
            for (GeneratedItineraryPayload.TransportSuggestion t : it.getTransportSuggestions()) {
                md.append("### ").append(safe(t.getMode()))
                        .append("（").append(safe(t.getPriority())).append("）\n\n");
                md.append("- 线路：").append(safe(t.getRoute())).append("\n");
                md.append("- 时长：").append(safe(t.getDuration())).append("\n");
                md.append("- 班次：").append(safe(t.getScheduleHint())).append("\n");
                md.append("- 票价：").append(safe(t.getPriceHint())).append("\n");
                if (StringUtils.hasText(t.getNote())) {
                    md.append("- 说明：").append(t.getNote()).append("\n");
                }
                md.append("\n");
            }
        }

        if (it != null && it.getDays() != null) {
            md.append("## 每日安排\n\n");
            for (GeneratedItineraryPayload.DayPlan day : it.getDays()) {
                md.append("### 第 ").append(day.getDayIndex()).append(" 天 · ")
                        .append(safe(day.getCity()));
                if (StringUtils.hasText(day.getTheme())) {
                    md.append("（").append(day.getTheme()).append("）");
                }
                md.append("\n\n");
                if (StringUtils.hasText(day.getAccommodationArea())) {
                    md.append("住宿建议：").append(day.getAccommodationArea()).append("\n\n");
                }
                if (day.getSlots() != null) {
                    for (GeneratedItineraryPayload.TimeSlot slot : day.getSlots()) {
                        md.append("- **").append(safe(slot.getPeriod())).append("** ")
                                .append(safe(slot.getPoiName())).append("：")
                                .append(safe(slot.getActivity()));
                        if (slot.getDurationMinutes() > 0) {
                            md.append("（约 ").append(slot.getDurationMinutes()).append(" 分钟）");
                        }
                        md.append("\n");
                        if (StringUtils.hasText(slot.getTransport())) {
                            md.append("  - 交通：").append(slot.getTransport()).append("\n");
                        }
                        if (StringUtils.hasText(slot.getTips())) {
                            md.append("  - 提示：").append(slot.getTips()).append("\n");
                        }
                    }
                }
                md.append("\n");
            }
        }

        if (it != null && it.getFoodRecommendations() != null && !it.getFoodRecommendations().isEmpty()) {
            md.append("## 美食推荐\n\n");
            for (String food : it.getFoodRecommendations()) {
                md.append("- ").append(food).append("\n");
            }
            md.append("\n");
        }

        Map<String, Object> budget = trip.getBudget() != null ? trip.getBudget()
                : (it != null ? it.getBudget() : null);
        if (budget != null) {
            md.append("## 预算估算\n\n");
            md.append("- 人均每日：¥").append(budget.get("perPersonPerDayMin"))
                    .append("–").append(budget.get("perPersonPerDayMax")).append("\n");
            md.append("- 全程：¥").append(budget.get("totalMin"))
                    .append("–").append(budget.get("totalMax")).append("\n");
            if (budget.get("note") != null) {
                md.append("- ").append(budget.get("note")).append("\n");
            }
            md.append("\n");
        }

        if (it != null && it.getWarnings() != null && !it.getWarnings().isEmpty()) {
            md.append("## 注意事项\n\n");
            for (String w : it.getWarnings()) {
                md.append("- ").append(w).append("\n");
            }
            md.append("\n");
        }

        md.append("---\n*由旅游神器生成，票价与时刻请以 12306 / 航司 / 景区官网为准。不代订票。*\n");
        return md.toString();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
