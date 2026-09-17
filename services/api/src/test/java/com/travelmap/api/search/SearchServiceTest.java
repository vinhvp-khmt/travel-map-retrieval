package com.travelmap.api.search;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.repository.SpatialCandidateProjection;
import com.travelmap.api.poi.service.PoiNameNormalizer;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.repository.SearchLogRepository;
import com.travelmap.api.search.service.DiversityReranker;
import com.travelmap.api.search.service.QueryNormalizer;
import com.travelmap.api.search.service.RankingService;
import com.travelmap.api.search.service.SearchService;
import com.travelmap.api.search.service.SimpleVietnameseTokenizer;
import com.travelmap.api.search.service.TemporalFitService;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceTest {
    @Test
    void intersectsTextAndSpatialCandidatesAndLogsSearch() {
        PoiRepository pois = mock(PoiRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        SearchLogRepository logs = mock(SearchLogRepository.class);
        var tokenizer = new SimpleVietnameseTokenizer();
        SearchService service = new SearchService(pois, categories, logs, tokenizer,
                new QueryNormalizer(tokenizer), new SearchRequestValidator(), new TemporalFitService(),
                new RankingService(), new DiversityReranker(new PoiNameNormalizer()));
        PoiEntity cafe = activeCafe();
        SpatialCandidateProjection candidate = mock(SpatialCandidateProjection.class);
        when(candidate.getId()).thenReturn(cafe.getId());
        when(candidate.getDistanceMeters()).thenReturn(250.0);
        when(pois.findAllByStatus(PoiStatus.ACTIVE)).thenReturn(List.of(cafe));
        when(pois.findSpatialCandidates(10.77, 106.70, 2_000, null, null, 500))
                .thenReturn(List.of(candidate));
        when(pois.findAllByIdIn(List.of(cafe.getId()))).thenReturn(List.of(cafe));
        var criteria = new SearchCriteria("cà phê", 10.77, 106.70, 2, null, 0, 20, null, null);

        var response = service.search(criteria);

        assertEquals(1, response.total());
        assertEquals(cafe.getId(), response.results().getFirst().poiId());
        assertNull(response.suggestion());
        verify(logs).save(criteria, "ca phe", 1);
        verify(pois).findAllByStatus(PoiStatus.ACTIVE);
    }

    @Test
    void returnsSuggestionWhenTextDoesNotMatch() {
        PoiRepository pois = mock(PoiRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        SearchLogRepository logs = mock(SearchLogRepository.class);
        var tokenizer = new SimpleVietnameseTokenizer();
        SearchService service = new SearchService(pois, categories, logs, tokenizer,
                new QueryNormalizer(tokenizer), new SearchRequestValidator(), new TemporalFitService(),
                new RankingService(), new DiversityReranker(new PoiNameNormalizer()));
        when(pois.findAllByStatus(PoiStatus.ACTIVE)).thenReturn(List.of());
        when(pois.findSpatialCandidates(10.77, 106.70, 2_000, null, null, 500)).thenReturn(List.of());
        when(pois.findAllByIdIn(List.of())).thenReturn(List.of());

        var response = service.search(new SearchCriteria("không tồn tại", 10.77, 106.70, 2,
                null, 0, 20, null, null));

        assertEquals(0, response.total());
        assertEquals("Try a broader radius or fewer filters", response.suggestion());
    }

    private static PoiEntity activeCafe() {
        var owner = new UserEntity("owner@example.com", "hash", UserRole.OWNER);
        var category = new CategoryEntity(UUID.randomUUID(), "Cà phê", "ca-phe");
        var poi = new PoiEntity(owner, category, "Cà phê yên tĩnh", "ca phe yen tinh",
                "Không gian làm việc", 10.77, 106.70, "Quận 1", 2, 40, false);
        poi.replaceOpeningHours(List.of(new PoiOpeningHourEntity(1, LocalTime.MIN, LocalTime.MAX, false)));
        poi.approve();
        return poi;
    }
}
