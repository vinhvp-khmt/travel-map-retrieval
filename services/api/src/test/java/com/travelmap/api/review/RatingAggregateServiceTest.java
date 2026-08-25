package com.travelmap.api.review;

import com.travelmap.api.review.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RatingAggregateServiceTest {
    @Test void refreshesAggregateThenReindexesPoi() {
        var jdbc = mock(JdbcTemplate.class); var reindex = mock(ReindexService.class); var id = UUID.randomUUID();
        new RatingAggregateService(jdbc, reindex).refresh(id);
        verify(jdbc).update(anyString(), eq(id), eq(id), eq(id));
        verify(reindex).reindex(id);
    }
}
