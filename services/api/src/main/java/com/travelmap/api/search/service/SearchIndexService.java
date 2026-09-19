package com.travelmap.api.search.service;

import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns the immutable-at-read-time BM25 index snapshot used by every search request.
 * Rebuilds replace the whole snapshot atomically, so concurrent readers never observe
 * a partially cleared index.
 */
@Service
public class SearchIndexService {
    private final PoiRepository poiRepository;
    private final VietnameseTokenizer tokenizer;
    private volatile InvertedIndex snapshot = new InvertedIndex();

    public SearchIndexService(PoiRepository poiRepository, VietnameseTokenizer tokenizer) {
        this.poiRepository = poiRepository;
        this.tokenizer = tokenizer;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void initialize() {
        rebuild();
    }

    @Transactional(readOnly = true)
    public synchronized void rebuild() {
        rebuild(poiRepository.findAllByStatus(PoiStatus.ACTIVE));
    }

    public synchronized void rebuild(List<PoiEntity> activePois) {
        Map<UUID, List<String>> documents = new HashMap<>();
        for (PoiEntity poi : activePois) {
            String text = poi.getName() + " " + (poi.getDescription() == null ? "" : poi.getDescription())
                    + " " + poi.getCategory().getName() + " " + poi.getAddress();
            documents.put(poi.getId(), tokenizer.tokenize(text));
        }
        InvertedIndex next = new InvertedIndex();
        next.rebuild(documents);
        snapshot = next;
    }

    public InvertedIndex snapshot() {
        return snapshot;
    }
}
