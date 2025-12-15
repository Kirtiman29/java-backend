package com.rdc.admin.repository;

import com.rdc.admin.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    // Custom find methods if needed, e.g., findByCode()
}