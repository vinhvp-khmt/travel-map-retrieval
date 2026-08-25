package com.travelmap.api.poi.repository;

import java.util.UUID;

public interface SpatialCandidateProjection {
    UUID getId();
    double getDistanceMeters();
}
