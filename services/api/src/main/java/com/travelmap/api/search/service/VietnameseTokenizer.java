package com.travelmap.api.search.service;

import java.util.List;

public interface VietnameseTokenizer {
    String normalize(String text);
    List<String> tokenize(String text);
}
