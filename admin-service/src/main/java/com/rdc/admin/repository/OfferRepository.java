package com.rdc.admin.repository;

import com.rdc.admin.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    boolean existsByCode(String code);
    Optional<Offer> findByCode(String code);
}