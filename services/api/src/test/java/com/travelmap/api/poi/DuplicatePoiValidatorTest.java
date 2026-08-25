package com.travelmap.api.poi;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.validation.DuplicatePoiValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DuplicatePoiValidatorTest {
    @Test
    void distanceBelowFiftyMetersIsDuplicate() {
        assertTrue(DuplicatePoiValidator.isDuplicateDistance(49.0));
    }

    @Test
    void distanceExactlyFiftyMetersIsAllowed() {
        assertFalse(DuplicatePoiValidator.isDuplicateDistance(50.0));
    }

    @Test
    void postGisDuplicateResultIsRejected() {
        PoiRepository repository = mock(PoiRepository.class);
        when(repository.existsDuplicateWithinMeters("ca phe", 10.77, 106.69, 50.0)).thenReturn(true);
        DuplicatePoiValidator validator = new DuplicatePoiValidator(repository);

        ApiException exception = assertThrows(ApiException.class,
                () -> validator.validate(null, "ca phe", 10.77, 106.69));
        assertEquals("POI_DUPLICATE_WITHIN_50_METERS", exception.getCode());
    }

    @Test
    void postGisNonDuplicateResultIsAllowed() {
        PoiRepository repository = mock(PoiRepository.class);
        DuplicatePoiValidator validator = new DuplicatePoiValidator(repository);
        assertDoesNotThrow(() -> validator.validate(null, "ca phe", 10.77, 106.69));
    }
}
