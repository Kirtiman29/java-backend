package com.rdc.admin.service;

import com.rdc.admin.entity.Fabric;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FabricPricingService {

    public Double calculateFinalPrice(Double basePrice, Integer discountPercent) {
        if (basePrice == null) {
            return null;
        }

        int safeDiscount = discountPercent != null ? Math.max(discountPercent, 0) : 0;
        BigDecimal price = BigDecimal.valueOf(basePrice);
        BigDecimal multiplier = BigDecimal.valueOf(100 - safeDiscount)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        return price.multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public Double calculateFinalPriceForMeter(Fabric fabric) {
        return calculateFinalPrice(fabric.getPricePerMeter(), fabric.getDiscountPercent());
    }

    public Double calculateFinalPriceForSwatch(Fabric fabric) {
        return calculateFinalPrice(fabric.getPricePerSwatch(), fabric.getDiscountPercent());
    }

    public Double calculateFinalPriceForQuarter(Fabric fabric) {
        return calculateFinalPrice(fabric.getPricePerQuarter(), fabric.getDiscountPercent());
    }

    public Double calculateFinalPriceForYard(Fabric fabric) {
        return calculateFinalPrice(fabric.getPricePerYard(), fabric.getDiscountPercent());
    }
}
