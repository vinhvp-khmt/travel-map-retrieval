package com.travelmap.api.booking.service;

import com.travelmap.api.poi.model.PoiEntity;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DepositPolicy {
    private static final BigDecimal RATE = new BigDecimal("0.10");
    public BigDecimal calculate(PoiEntity poi, int partySize) {
        int level = poi.getPriceLevel() == null ? 1 : poi.getPriceLevel();
        BigDecimal estimatedPerGuest = BigDecimal.valueOf(100_000L * level);
        return estimatedPerGuest.multiply(BigDecimal.valueOf(partySize)).multiply(RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
