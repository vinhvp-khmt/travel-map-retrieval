package com.travelmap.api.search;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchRequestValidatorTest {
    private final SearchRequestValidator validator = new SearchRequestValidator();

    @Test
    void acceptsDefaults() {
        assertDoesNotThrow(() -> validator.validate(new SearchCriteria(
                "cà phê", 10.77, 106.70, 2, null, 0, 20, null, null)));
    }

    @Test
    void rejectsRadiusAboveTenKilometres() {
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, 106.70, 10.1, null, 0, 20, null, null)));
    }

    @Test
    void rejectsPageSizeAboveFifty() {
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, 106.70, 2, null, 0, 51, null, null)));
    }

    @Test
    void acceptsMissingGps() {
        // Phần 2.4: cả hai cùng null (không truyền GPS) phải hợp lệ — search vẫn chạy được.
        assertDoesNotThrow(() -> validator.validate(new SearchCriteria(
                "cafe", null, null, 2, null, 0, 20, null, null)));
    }

    @Test
    void rejectsHalfMissingGps() {
        // Có latitude mà thiếu longitude (hoặc ngược lại) là request sai, không phải "không
        // có GPS" — phải báo lỗi rõ ràng thay vì im lặng bỏ qua tín hiệu khoảng cách.
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, null, 2, null, 0, 20, null, null)));
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", null, 106.70, 2, null, 0, 20, null, null)));
    }

    @Test
    void rejectsOutOfRangeGpsWhenPresent() {
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 91.0, 106.70, 2, null, 0, 20, null, null)));
        assertThrows(ApiException.class, () -> validator.validate(new SearchCriteria(
                "cafe", 10.77, 181.0, 2, null, 0, 20, null, null)));
    }
}
