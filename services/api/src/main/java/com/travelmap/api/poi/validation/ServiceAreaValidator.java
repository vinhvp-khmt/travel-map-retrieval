package com.travelmap.api.poi.validation;

import com.travelmap.api.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ServiceAreaValidator {
    private final double minLatitude;
    private final double maxLatitude;
    private final double minLongitude;
    private final double maxLongitude;

    public ServiceAreaValidator(
            @Value("${travelmap.poi.service-area.min-latitude:10.70}") double minLatitude,
            @Value("${travelmap.poi.service-area.max-latitude:10.90}") double maxLatitude,
            @Value("${travelmap.poi.service-area.min-longitude:106.55}") double minLongitude,
            @Value("${travelmap.poi.service-area.max-longitude:106.85}") double maxLongitude) {
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    public void validate(double latitude, double longitude) {
        if (latitude < minLatitude || latitude > maxLatitude
                || longitude < minLongitude || longitude > maxLongitude) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "POI_OUTSIDE_SERVICE_AREA",
                    "Coordinates are outside the configured service area");
        }
    }
}
