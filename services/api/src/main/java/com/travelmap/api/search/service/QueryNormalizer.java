package com.travelmap.api.search.service;

import org.springframework.stereotype.Component;

@Component
public class QueryNormalizer {
    private final VietnameseTokenizer tokenizer;

    public QueryNormalizer(VietnameseTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String normalize(String query) {
        return tokenizer.normalize(query);
    }
}
