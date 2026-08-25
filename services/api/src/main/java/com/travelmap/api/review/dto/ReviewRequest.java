package com.travelmap.api.review.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record ReviewRequest(
        @Min(1) @Max(5) int rating,
        @Size(max = 2000) String comment,
        @Size(max = 5) List<@Pattern(regexp = "https?://.+", message = "must be an HTTP(S) URL") String> imageUrls) {
    public ReviewRequest { imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls); }
}
