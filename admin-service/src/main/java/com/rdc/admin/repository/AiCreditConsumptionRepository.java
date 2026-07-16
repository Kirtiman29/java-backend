package com.rdc.admin.repository;

import com.rdc.admin.entity.AiCreditConsumption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiCreditConsumptionRepository extends JpaRepository<AiCreditConsumption, Long> {
    Optional<AiCreditConsumption> findByJobId(Long jobId);
}
