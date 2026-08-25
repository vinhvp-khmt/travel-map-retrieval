package com.travelmap.api.search;

import com.travelmap.api.search.service.SimpleVietnameseTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseTokenizerTest {
    private final SimpleVietnameseTokenizer tokenizer = new SimpleVietnameseTokenizer();

    @Test
    void normalizesVietnameseAccentsAndPunctuation() {
        assertEquals("ca phe dep quan 1", tokenizer.normalize("  Cà-phê ĐẸP, Quận 1! "));
    }

    @Test
    void removesStopWordsAndSingleCharacters() {
        assertEquals(List.of("quan", "ngon"), tokenizer.tokenize("ở quán và ngon"));
    }
}
