package com.travelmap.api.search.service;

import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Service
public class TemporalFitService {
    public TemporalFit evaluate(PoiEntity poi, OffsetDateTime visitAt) {
        int today = visitAt.getDayOfWeek().getValue();
        int yesterday = today == 1 ? 7 : today - 1;
        LocalTime time = visitAt.toLocalTime();

        for (PoiOpeningHourEntity hour : poi.getOpeningHours()) {
            if (hour.isClosed()) continue;
            if (hour.getDayOfWeek() == today && isOpenFromToday(hour, time)) return new TemporalFit(1.0, true);
            if (hour.getDayOfWeek() == yesterday && hour.isSpansNextDay()
                    && time.isBefore(hour.getCloseTime())) return new TemporalFit(1.0, true);
        }
        for (PoiOpeningHourEntity hour : poi.getOpeningHours()) {
            if (!hour.isClosed() && hour.getDayOfWeek() == today && time.isBefore(hour.getOpenTime())
                    && java.time.Duration.between(time, hour.getOpenTime()).toMinutes() <= 60) {
                return new TemporalFit(0.5, false);
            }
        }
        return new TemporalFit(0.0, false);
    }

    private static boolean isOpenFromToday(PoiOpeningHourEntity hour, LocalTime time) {
        if (time.isBefore(hour.getOpenTime())) return false;
        return hour.isSpansNextDay() || time.isBefore(hour.getCloseTime());
    }

    public record TemporalFit(double score, boolean open) { }
}
