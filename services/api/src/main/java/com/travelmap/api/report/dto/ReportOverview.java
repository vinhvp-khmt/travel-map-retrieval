package com.travelmap.api.report.dto;

import java.math.BigDecimal;

public record ReportOverview(long poiCount, long bookingCount, long completedBookingCount,
                             BigDecimal conversionRatePercent, BigDecimal paidRevenue,
                             long reviewCount, BigDecimal averageRating) { }
