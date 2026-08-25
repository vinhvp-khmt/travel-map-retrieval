package com.travelmap.api.search.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InvertedIndex {
    private final Map<String, Map<UUID, Integer>> postings = new HashMap<>();
    private final Map<UUID, Integer> documentLengths = new HashMap<>();

    public void rebuild(Map<UUID, List<String>> documents) {
        postings.clear();
        documentLengths.clear();
        documents.forEach((id, terms) -> {
            documentLengths.put(id, terms.size());
            terms.forEach(term -> postings.computeIfAbsent(term, ignored -> new HashMap<>())
                    .merge(id, 1, Integer::sum));
        });
    }

    public int termFrequency(String term, UUID documentId) {
        return postings.getOrDefault(term, Map.of()).getOrDefault(documentId, 0);
    }

    public int documentFrequency(String term) {
        return postings.getOrDefault(term, Map.of()).size();
    }

    public int documentLength(UUID documentId) {
        return documentLengths.getOrDefault(documentId, 0);
    }

    public int documentCount() { return documentLengths.size(); }

    public double averageDocumentLength() {
        return documentLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    public Set<UUID> documentIds() { return Set.copyOf(documentLengths.keySet()); }
}
