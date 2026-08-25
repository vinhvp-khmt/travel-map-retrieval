package com.travelmap.api.poi.dto;

import com.travelmap.api.poi.model.PoiEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PoiResponse(
        UUID id,
        UUID ownerId,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        double latitude,
        double longitude,
        String address,
        Integer priceLevel,
        Integer capacity,
        boolean bookingEnabled,
        String status,
        BigDecimal averageRating,
        int ratingCount,
        List<OpeningHourResponse> openingHours
) {
    public static PoiResponse from(PoiEntity entity) {
        return new PoiResponse(entity.getId(), entity.getOwner().getId(), entity.getCategory().getId(),
                entity.getCategory().getName(), entity.getName(), entity.getDescription(), entity.getLatitude(),
                entity.getLongitude(), entity.getAddress(), entity.getPriceLevel(), entity.getCapacity(),
                entity.isBookingEnabled(), entity.getStatus().name(), entity.getAvgRating(), entity.getRatingCount(),
                entity.getOpeningHours().stream().map(OpeningHourResponse::from).toList());
    }
}
