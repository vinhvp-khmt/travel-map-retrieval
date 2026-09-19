package com.travelmap.api.review;

import com.travelmap.api.review.service.ReindexService;
import com.travelmap.api.search.service.SearchIndexService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ReindexServiceTest {
    @Test
    void refreshesThePersistedDocumentAndLiveIndexSnapshot() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        SearchIndexService searchIndex = mock(SearchIndexService.class);
        UUID poiId = UUID.randomUUID();

        new ReindexService(jdbc, searchIndex).reindex(poiId);

        verify(jdbc).update(anyString(), org.mockito.ArgumentMatchers.eq(poiId));
        verify(searchIndex).rebuild();
    }
}
