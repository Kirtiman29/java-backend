package com.rdc.admin.service;

import com.rdc.admin.entity.Design;
import org.springframework.stereotype.Service;

@Service
public class DesignPricingService {

    public int calculateFinalPrice(Design design) {
        if (design.getBasePriceCents() == null) return 0;

        int price = design.getBasePriceCents();

        if (Boolean.TRUE.equals(design.getSpecialOffer())
                && design.getDiscountPercent() != null
                && design.getDiscountPercent() > 0) {

            double discountMultiplier = (100.0 - design.getDiscountPercent()) / 100.0;
            price = (int) Math.round(price * discountMultiplier);
        }

        return price;
    }
}