package com.travelmap.api.search.validation;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.search.model.SearchCriteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SearchRequestValidator {
    public void validate(SearchCriteria criteria) {
        if (criteria.query() == null || criteria.query().trim().isEmpty() || criteria.query().trim().length() > 200)
            invalid("query must contain 1 to 200 characters");
        if (criteria.latitude() < -90 || criteria.latitude() > 90) invalid("latitude must be between -90 and 90");
        if (criteria.longitude() < -180 || criteria.longitude() > 180) invalid("longitude must be between -180 and 180");
        if (criteria.radiusKm() < 0.1 || criteria.radiusKm() > 10) invalid("radiusKm must be between 0.1 and 10");
        if (criteria.page() < 0) invalid("page must be at least 0");
        if (criteria.size() < 1 || criteria.size() > 50) invalid("size must be between 1 and 50");
        if (criteria.priceLevel() != null && (criteria.priceLevel() < 1 || criteria.priceLevel() > 4))
            invalid("priceLevel must be between 1 and 4");
        if (criteria.visitAt() != null && criteria.visitAt().isBefore(OffsetDateTime.now().minusMinutes(1)))
            invalid("visitAt must be now or in the future");
    }

    private static void invalid(String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "SEARCH_VALIDATION_ERROR", message);
    }
}
