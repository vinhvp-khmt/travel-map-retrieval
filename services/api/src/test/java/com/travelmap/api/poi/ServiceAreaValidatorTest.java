package com.travelmap.api.poi;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.validation.ServiceAreaValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceAreaValidatorTest {
    private final ServiceAreaValidator validator = new ServiceAreaValidator(10.70, 10.90, 106.55, 106.85);

    @Test
    void centralHoChiMinhCityCoordinateIsAccepted() {
        assertDoesNotThrow(() -> validator.validate(10.7769, 106.7009));
    }

    @Test
    void coordinateOutsideConfiguredAreaIsRejected() {
        assertThrows(ApiException.class, () -> validator.validate(21.0285, 105.8542));
    }
}
