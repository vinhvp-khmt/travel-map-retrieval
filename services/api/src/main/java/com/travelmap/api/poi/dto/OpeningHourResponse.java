package com.travelmap.api.poi.dto;

import com.travelmap.api.poi.model.PoiOpeningHourEntity;

import java.time.LocalTime;

public record OpeningHourResponse(
        int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed,
        boolean spansNextDay
) {
    static OpeningHourResponse from(PoiOpeningHourEntity entity) {
        return new OpeningHourResponse(entity.getDayOfWeek(), entity.getOpenTime(), entity.getCloseTime(),
                entity.isClosed(), entity.isSpansNextDay());
    }
}
