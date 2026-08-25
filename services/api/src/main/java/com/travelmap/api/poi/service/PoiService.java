package com.travelmap.api.poi.service;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.dto.OpeningHourRequest;
import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.dto.PoiUpsertRequest;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.validation.DuplicatePoiValidator;
import com.travelmap.api.poi.validation.OpeningHoursValidator;
import com.travelmap.api.poi.validation.ServiceAreaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PoiService {
    private final PoiRepository poiRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PoiNameNormalizer nameNormalizer;
    private final ServiceAreaValidator serviceAreaValidator;
    private final DuplicatePoiValidator duplicatePoiValidator;
    private final OpeningHoursValidator openingHoursValidator;

    public PoiService(PoiRepository poiRepository, CategoryRepository categoryRepository, UserRepository userRepository,
                      PoiNameNormalizer nameNormalizer, ServiceAreaValidator serviceAreaValidator,
                      DuplicatePoiValidator duplicatePoiValidator, OpeningHoursValidator openingHoursValidator) {
        this.poiRepository = poiRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.nameNormalizer = nameNormalizer;
        this.serviceAreaValidator = serviceAreaValidator;
        this.duplicatePoiValidator = duplicatePoiValidator;
        this.openingHoursValidator = openingHoursValidator;
    }

    @Transactional
    public PoiResponse create(String ownerEmail, PoiUpsertRequest request) {
        UserEntity owner = requireUser(ownerEmail);
        CategoryEntity category = requireCategory(request.categoryId());
        String normalizedName = validateRequest(null, request);
        PoiEntity poi = new PoiEntity(owner, category, request.name().trim(), normalizedName,
                trimToNull(request.description()), request.latitude(), request.longitude(), request.address().trim(),
                request.priceLevel(), request.capacity(), request.bookingEnabled());
        poi.replaceOpeningHours(toEntities(request.openingHours()));
        return PoiResponse.from(poiRepository.save(poi));
    }

    @Transactional
    public PoiResponse update(String ownerEmail, UUID poiId, PoiUpsertRequest request) {
        PoiEntity poi = poiRepository.findByIdAndOwner_EmailIgnoreCase(poiId, ownerEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found"));
        CategoryEntity category = requireCategory(request.categoryId());
        String normalizedName = validateRequest(poiId, request);
        poi.apply(category, request.name().trim(), normalizedName, trimToNull(request.description()),
                request.latitude(), request.longitude(), request.address().trim(), request.priceLevel(),
                request.capacity(), request.bookingEnabled());
        poi.replaceOpeningHours(toEntities(request.openingHours()));
        poi.markPendingApproval();
        return PoiResponse.from(poi);
    }

    @Transactional(readOnly = true)
    public List<PoiResponse> listOwned(String ownerEmail) {
        return poiRepository.findAllByOwner_EmailIgnoreCaseOrderByCreatedAtDesc(ownerEmail)
                .stream().map(PoiResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PoiResponse getActive(UUID poiId) {
        return poiRepository.findByIdAndStatus(poiId, PoiStatus.ACTIVE)
                .map(PoiResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found"));
    }

    private String validateRequest(UUID excludedId, PoiUpsertRequest request) {
        serviceAreaValidator.validate(request.latitude(), request.longitude());
        openingHoursValidator.validate(request.openingHours());
        String normalizedName = nameNormalizer.normalize(request.name());
        duplicatePoiValidator.validate(excludedId, normalizedName, request.latitude(), request.longitude());
        return normalizedName;
    }

    private UserEntity requireUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Authenticated user was not found"));
    }

    private CategoryEntity requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Category does not exist"));
    }

    private static List<PoiOpeningHourEntity> toEntities(List<OpeningHourRequest> requests) {
        return requests.stream().map(request -> new PoiOpeningHourEntity(
                request.dayOfWeek(), request.openTime(), request.closeTime(), request.closed())).toList();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
