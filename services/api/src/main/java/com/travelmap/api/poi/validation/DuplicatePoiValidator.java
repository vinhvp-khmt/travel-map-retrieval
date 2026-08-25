package com.travelmap.api.poi.validation;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.repository.PoiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DuplicatePoiValidator {
    public static final double MINIMUM_DISTANCE_METERS = 50.0;
    private final PoiRepository poiRepository;

    public DuplicatePoiValidator(PoiRepository poiRepository) {
        this.poiRepository = poiRepository;
    }

    public void validate(UUID excludedId, String normalizedName, double latitude, double longitude) {
        boolean duplicate = excludedId == null
                ? poiRepository.existsDuplicateWithinMeters(normalizedName, latitude, longitude, MINIMUM_DISTANCE_METERS)
                : poiRepository.existsDuplicateWithinMetersExcluding(
                        excludedId, normalizedName, latitude, longitude, MINIMUM_DISTANCE_METERS);
        if (duplicate) {
            throw new ApiException(HttpStatus.CONFLICT, "POI_DUPLICATE_WITHIN_50_METERS",
                    "A POI with the same normalized name exists within 50 meters");
        }
    }

    public static boolean isDuplicateDistance(double distanceMeters) {
        return distanceMeters < MINIMUM_DISTANCE_METERS;
    }
}
