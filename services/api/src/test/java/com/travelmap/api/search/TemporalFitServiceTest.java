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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // ===== Phase 2.5 (IR search plan) — đủ 10 mốc biên cho quán 18:00 → 02:00 =====
    // 09:00/17:59 CLOSED, 18:00..23:59 OPEN (cùng ngày), 00:00..01:59 OPEN (qua đêm),
    // 02:00/02:01/03:00 CLOSED (đóng đúng biên, không đóng chậm hơn 1 phút).

    @Test
    void quaDem18h_02h_dungBienTruocGio() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false));
        assertFalse(service.evaluate(poi, futureMonday().withHour(9).withMinute(0)).open(), "09:00 phải CLOSED");
        assertFalse(service.evaluate(poi, futureMonday().withHour(17).withMinute(59)).open(), "17:59 phải CLOSED");
    }

    @Test
    void quaDem18h_02h_openCungNgay() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false));
        assertTrue(service.evaluate(poi, futureMonday().withHour(18).withMinute(0)).open(), "18:00 phải OPEN");
        assertTrue(service.evaluate(poi, futureMonday().withHour(23).withMinute(59)).open(), "23:59 phải OPEN");
    }

    @Test
    void quaDem18h_02h_openSangHomSau() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false));
        OffsetDateTime tuesday = futureMonday().plusDays(1);
        assertTrue(service.evaluate(poi, tuesday.withHour(0).withMinute(0)).open(), "00:00 phải OPEN");
        assertTrue(service.evaluate(poi, tuesday.withHour(0).withMinute(30)).open(), "00:30 phải OPEN");
        assertTrue(service.evaluate(poi, tuesday.withHour(1).withMinute(59)).open(), "01:59 phải OPEN");
    }

    @Test
    void quaDem18h_02h_dongDungBienSauGio() {
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false));
        OffsetDateTime tuesday = futureMonday().plusDays(1);
        assertFalse(service.evaluate(poi, tuesday.withHour(2).withMinute(0)).open(), "02:00 phải CLOSED (biên đóng, loại trừ)");
        assertFalse(service.evaluate(poi, tuesday.withHour(2).withMinute(1)).open(), "02:01 phải CLOSED");
        assertFalse(service.evaluate(poi, tuesday.withHour(3).withMinute(0)).open(), "03:00 phải CLOSED");
    }

    @Test
    void gioBinhThuong08h_22h_dungBien() {
        // Case đơn giản 08:00→22:00 (không qua đêm) để đối chiếu với case qua đêm ở trên.
        PoiEntity poi = poiWithHours(new PoiOpeningHourEntity(1, LocalTime.of(8, 0), LocalTime.of(22, 0), false));
        assertFalse(service.evaluate(poi, futureMonday().withHour(7).withMinute(59)).open());
        assertTrue(service.evaluate(poi, futureMonday().withHour(8).withMinute(0)).open());
        assertTrue(service.evaluate(poi, futureMonday().withHour(21).withMinute(59)).open());
        assertFalse(service.evaluate(poi, futureMonday().withHour(22).withMinute(0)).open());
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
