package com.travelmap.api.search.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SimpleVietnameseTokenizer implements VietnameseTokenizer {
    private static final Set<String> STOP_WORDS = Set.of("va", "la", "o", "tai", "mot", "nhung", "cua", "cho");

    @Override
    public String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text.trim().toLowerCase(Locale.ROOT).replace('đ', 'd'),
                Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    @Override
    public List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return List.of();
        return Arrays.stream(normalized.split(" "))
                .filter(term -> term.length() > 1 && !STOP_WORDS.contains(term))
                .toList();
    }
}
