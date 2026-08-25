package com.travelmap.api.poi;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.poi.dto.OpeningHourRequest;
import com.travelmap.api.poi.dto.PoiUpsertRequest;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.service.PoiNameNormalizer;
import com.travelmap.api.poi.service.PoiService;
import com.travelmap.api.poi.validation.DuplicatePoiValidator;
import com.travelmap.api.poi.validation.OpeningHoursValidator;
import com.travelmap.api.poi.validation.ServiceAreaValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoiServiceTest {
    @Test
    void ownerCreatesPendingPoiWithNormalizedName() {
        PoiRepository poiRepository = mock(PoiRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        ServiceAreaValidator serviceArea = mock(ServiceAreaValidator.class);
        DuplicatePoiValidator duplicate = mock(DuplicatePoiValidator.class);
        OpeningHoursValidator openingHours = mock(OpeningHoursValidator.class);
        UUID categoryId = UUID.randomUUID();
        UserEntity owner = new UserEntity("owner@example.com", "hash", UserRole.OWNER);
        CategoryEntity category = new CategoryEntity(categoryId, "Cà phê", "ca-phe");
        when(userRepository.findByEmailIgnoreCase("owner@example.com")).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(poiRepository.save(any(PoiEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PoiService service = new PoiService(poiRepository, categoryRepository, userRepository,
                new PoiNameNormalizer(), serviceArea, duplicate, openingHours);
        PoiUpsertRequest request = new PoiUpsertRequest(categoryId, "Cà Phê Yên Tĩnh", "Không gian làm việc",
                10.7769, 106.7009, "Quận 1", 2, 50, true,
                List.of(new OpeningHourRequest(1, LocalTime.of(8, 0), LocalTime.of(22, 0), false)));

        var response = service.create("owner@example.com", request);

        assertEquals("PENDING_APPROVAL", response.status());
        assertEquals("Cà Phê Yên Tĩnh", response.name());
        verify(duplicate).validate(null, "ca phe yen tinh", 10.7769, 106.7009);
    }
}
