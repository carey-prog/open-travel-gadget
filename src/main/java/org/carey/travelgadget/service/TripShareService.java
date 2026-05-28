package org.carey.travelgadget.service;

import lombok.RequiredArgsConstructor;
import org.carey.travelgadget.domain.dto.TripDetailDto;
import org.carey.travelgadget.domain.entity.Trip;
import org.carey.travelgadget.mapper.TripMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripShareService {

    private final TripMapper tripMapper;
    private final TripService tripService;

    @Transactional
    public Map<String, Object> createShareLink(Long tripId) {
        Trip trip = tripMapper.selectById(tripId);
        if (trip == null) {
            throw new IllegalStateException("行程不存在");
        }
        if (trip.getShareToken() == null || trip.getShareToken().isBlank()) {
            trip.setShareToken(UUID.randomUUID().toString().replace("-", ""));
            tripMapper.updateById(trip);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tripId", tripId);
        result.put("shareToken", trip.getShareToken());
        result.put("sharePath", "/s/" + trip.getShareToken());
        return result;
    }

    public TripDetailDto getByShareToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Trip trip = tripMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Trip>()
                        .eq(Trip::getShareToken, token.trim())
                        .last("LIMIT 1"));
        if (trip == null) {
            return null;
        }
        return tripService.getTripDetail(trip.getId());
    }
}
