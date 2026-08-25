package com.travelmap.api.poi;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.dto.OpeningHourRequest;
import com.travelmap.api.poi.validation.OpeningHoursValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpeningHoursValidatorTest {
    private final OpeningHoursValidator validator = new OpeningHoursValidator();

    @Test
    void overlappingIntervalsAreRejected() {
        var hours = List.of(
                new OpeningHourRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0), false),
                new OpeningHourRequest(1, LocalTime.of(11, 0), LocalTime.of(15, 0), false));
        assertThrows(ApiException.class, () -> validator.validate(hours));
    }

    @Test
    void overnightIntervalWithoutOverlapIsAccepted() {
        var hours = List.of(
                new OpeningHourRequest(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false),
                new OpeningHourRequest(2, LocalTime.of(8, 0), LocalTime.of(12, 0), false));
        assertDoesNotThrow(() -> validator.validate(hours));
    }

    @Test
    void closedDayCannotAlsoHaveOpenInterval() {
        var hours = List.of(
                new OpeningHourRequest(1, null, null, true),
                new OpeningHourRequest(1, LocalTime.of(8, 0), LocalTime.of(12, 0), false));
        assertThrows(ApiException.class, () -> validator.validate(hours));
    }
}
