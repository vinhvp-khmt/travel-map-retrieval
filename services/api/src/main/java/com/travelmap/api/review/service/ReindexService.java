package com.travelmap.api.review.service;

import com.travelmap.api.search.service.SearchIndexService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class ReindexService {
    private final JdbcTemplate jdbc;
    private final SearchIndexService searchIndexService;
    public ReindexService(JdbcTemplate jdbc, SearchIndexService searchIndexService) {
        this.jdbc = jdbc;
        this.searchIndexService = searchIndexService;
    }
    public void reindex(UUID poiId) {
        jdbc.update("""
            INSERT INTO poi_search_document(poi_id, normalized_text, document_length, indexed_at)
            SELECT p.id,
                   lower(concat_ws(' ', p.name, p.description, p.address, c.name)),
                   cardinality(regexp_split_to_array(trim(concat_ws(' ', p.name, p.description, p.address, c.name)), '\\s+')),
                   CURRENT_TIMESTAMP
              FROM poi p JOIN category c ON c.id = p.category_id WHERE p.id = ?
            ON CONFLICT (poi_id) DO UPDATE SET normalized_text = EXCLUDED.normalized_text,
                document_length = EXCLUDED.document_length, indexed_at = EXCLUDED.indexed_at
            """, poiId);
        searchIndexService.rebuild();
    }
}
