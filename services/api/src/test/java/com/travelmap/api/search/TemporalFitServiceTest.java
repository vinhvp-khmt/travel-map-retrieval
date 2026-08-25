package com.travelmap.api.search;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import com.travelmap.api.search.service.TemporalFitService;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static java.time.DayOfWeek.MONDAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporalFitServiceTest {
    private final TemporalFitService service = new TemporalFitService();

    @Test
    void openingWithinSixtyMinutesHasHalfFit() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(10, 0), LocalTime.of(18, 0), false));
        OffsetDateTime monday = futureMonday().withHour(9).withMinute(15);
        assertEquals(0.5, service.evaluate(poi, monday).score());
    }

    @Test
    void overnightHoursAreOpenAfterMidnight() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(22, 0), LocalTime.of(2, 0), false));
        OffsetDateTime tuesday = futureMonday().plusDays(1).withHour(1).withMinute(0);
        assertTrue(service.evaluate(poi, tuesday).open());
    }

    private static OffsetDateTime futureMonday() {
        return OffsetDateTime.now().plusDays(1).with(TemporalAdjusters.nextOrSame(MONDAY))
                .withSecond(0).withNano(0);
    }

    private static PoiEntity poiWithHours(PoiOpeningHourEntity hour) {
        var owner = new UserEntity("owner@example.com", "hash", UserRole.OWNER);
        var category = new CategoryEntity(UUID.randomUUID(), "Cafe", "cafe");
        var poi = new PoiEntity(owner, category, "Cafe", "cafe", null, 10.77, 106.70,
                "District 1", 2, 20, false);
        poi.replaceOpeningHours(List.of(hour));
        poi.approve();
        return poi;
    }
}
