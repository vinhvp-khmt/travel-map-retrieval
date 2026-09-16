package com.travelmap.api.search.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tiêu chí một lần tìm kiếm.
 *
 * <p>Trường {@code profile} (thêm ở phần 2.1) chọn công thức xếp hạng: {@code V1}
 * là bản gốc, {@code V2} là bản thử. Để KHÔNG phá vỡ mọi nơi đang tạo
 * {@code SearchCriteria} với 9 tham số (controller cũ, các test), có thêm một
 * constructor phụ 9 tham số mặc định {@link WeightProfile#V1}.
 */
public record SearchCriteria(
        String query,
        double latitude,
        double longitude,
        double radiusKm,
        OffsetDateTime visitAt,
        int page,
        int size,
        Integer priceLevel,
        UUID categoryId,
        WeightProfile profile
) {
    /**
     * Constructor tương thích ngược: bỏ qua {@code profile} thì mặc định {@link WeightProfile#V1}.
     * Nhờ vậy mã và test cũ (dùng 9 tham số) vẫn biên dịch nguyên vẹn.
     */
    public SearchCriteria(String query, double latitude, double longitude, double radiusKm,
                          OffsetDateTime visitAt, int page, int size,
                          Integer priceLevel, UUID categoryId) {
        this(query, latitude, longitude, radiusKm, visitAt, page, size,
                priceLevel, categoryId, WeightProfile.V1);
    }
}
