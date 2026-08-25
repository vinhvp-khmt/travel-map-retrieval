package com.travelmap.api.review;

import com.travelmap.api.review.dto.ReviewRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReviewRequestValidationTest {
    @Test void rejectsInvalidRatingAndNonHttpImage() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(new ReviewRequest(6, "ok", List.of("file:///tmp/image.jpg")));
            assertFalse(violations.isEmpty());
        }
    }
}
