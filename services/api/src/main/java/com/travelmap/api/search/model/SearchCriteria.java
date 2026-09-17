package com.travelmap.api.search.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tiêu chí một lần tìm kiếm.
 *
 * <p>Trường {@code profile} (thêm ở phần 2.1) chọn công thức xếp hạng: {@code V1}
 * là bản gốc, {@code V2} là bản thử.
 *
 * <p>Trường {@code diversify} (thêm ở phần 2.3) bật/tắt việc đa dạng hoá kết quả
 * trang đầu (gộp chi nhánh trùng tên + giới hạn số quán cùng loại). Mặc định {@code true}.
 *
 * <p>Để KHÔNG phá vỡ mọi nơi đang tạo {@code SearchCriteria} với ít tham số hơn
 * (controller, các test cũ), có thêm hai constructor phụ đặt mặc định
 * {@code profile = }{@link WeightProfile#V1} và {@code diversify = true}.
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
        WeightProfile profile,
        boolean diversify
) {
    /**
     * Constructor tương thích ngược 10 tham số: bỏ qua {@code diversify} thì mặc định {@code true}.
     * Nhờ vậy nơi nào đã truyền {@code profile} (như controller cũ) vẫn biên dịch nguyên vẹn.
     */
    public SearchCriteria(String query, double latitude, double longitude, double radiusKm,
                          OffsetDateTime visitAt, int page, int size,
                          Integer priceLevel, UUID categoryId, WeightProfile profile) {
        this(query, latitude, longitude, radiusKm, visitAt, page, size,
                priceLevel, categoryId, profile, true);
    }

    /**
     * Constructor tương thích ngược 9 tham số: bỏ qua cả {@code profile} lẫn {@code diversify}
     * thì mặc định {@link WeightProfile#V1} và {@code diversify = true}.
     * Nhờ vậy mã và test cũ (dùng 9 tham số) vẫn biên dịch nguyên vẹn.
     */
    public SearchCriteria(String query, double latitude, double longitude, double radiusKm,
                          OffsetDateTime visitAt, int page, int size,
                          Integer priceLevel, UUID categoryId) {
        this(query, latitude, longitude, radiusKm, visitAt, page, size,
                priceLevel, categoryId, WeightProfile.V1, true);
    }
}
