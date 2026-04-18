package com.rdc.subscription.repository;

import com.rdc.subscription.entity.CreditWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CreditWalletRepository extends JpaRepository<CreditWallet, Long> {
    Optional<CreditWallet> findByUserId(Long userId);
}