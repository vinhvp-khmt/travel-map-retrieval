package com.travelmap.api.search;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchRequestValidatorTest {
    private final SearchRequestValidator validator = new SearchRequestValidator();

    @Test
    void acceptsDefaults() {
        assertDoesNotThrow(() -> validator.validate(new SearchCriteria(
                "cà phê", 10.77, 106.70, 2, null, 0, 20, null, null)));
    }

    @Test
    void rejectsRadiusAboveTenKilometres() {
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, 106.70, 10.1, null, 0, 20, null, null)));
    }

    @Test
    void rejectsPageSizeAboveFifty() {
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, 106.70, 2, null, 0, 51, null, null)));
    }
}
