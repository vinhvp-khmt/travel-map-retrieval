package com.travelmap.api.search;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.search.service.SearchIndexService;
import com.travelmap.api.search.service.SimpleVietnameseTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchIndexServiceTest {
    @Test
    void rebuildAtomicallyReplacesTheActivePoiSnapshot() {
        PoiRepository repository = mock(PoiRepository.class);
        var tokenizer = new SimpleVietnameseTokenizer();
        var service = new SearchIndexService(repository, tokenizer);
        PoiEntity first = poi("Cà phê yên tĩnh");
        PoiEntity second = poi("Rooftop ngắm hoàng hôn");

        service.rebuild(List.of(first));
        assertEquals(1, service.snapshot().documentCount());
        assertEquals(1, service.snapshot().documentFrequency("yen"));

        service.rebuild(List.of(second));
        assertEquals(1, service.snapshot().documentCount());
        assertEquals(0, service.snapshot().documentFrequency("yen"));
        assertEquals(1, service.snapshot().documentFrequency("rooftop"));
    }

    @Test
    void repositoryRebuildLoadsOnlyActivePois() {
        PoiRepository repository = mock(PoiRepository.class);
        PoiEntity active = poi("Cafe làm việc");
        when(repository.findAllByStatus(PoiStatus.ACTIVE)).thenReturn(List.of(active));
        var service = new SearchIndexService(repository, new SimpleVietnameseTokenizer());

        service.rebuild();

        assertEquals(1, service.snapshot().documentCount());
        verify(repository).findAllByStatus(PoiStatus.ACTIVE);
    }

    private static PoiEntity poi(String name) {
        PoiEntity poi = new PoiEntity(
                new UserEntity("owner@example.com", "hash", UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(), "Cà phê", "ca-phe"),
                name, name.toLowerCase(), "Mô tả giàu từ khóa", 10.77, 106.70,
                "Quận 1", 2, 30, false);
        poi.approve();
        return poi;
    }
}
