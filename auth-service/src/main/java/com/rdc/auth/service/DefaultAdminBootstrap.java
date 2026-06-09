package com.rdc.auth.service;

import com.rdc.auth.entity.Admin;
import com.rdc.auth.repository.AdminRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultAdminBootstrap implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.email:developer@ruchitadesigncompany.com}")
    private String defaultAdminEmail;

    @Value("${app.default-admin.password:Admin@Rdc2020}")
    private String defaultAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        adminRepository.findByEmail(defaultAdminEmail)
                .ifPresentOrElse(
                        admin -> log.info("Default admin account is present for {}", defaultAdminEmail),
                        this::createDefaultAdminIfSafe
                );
    }

    private void createDefaultAdminIfSafe() {
        long adminCount = adminRepository.count();

        if (adminCount > 0) {
            log.warn(
                    "Default admin {} is missing, but {} admin record(s) already exist. Skipping automatic bootstrap.",
                    defaultAdminEmail,
                    adminCount
            );
            return;
        }

        Admin admin = Admin.builder()
                .email(defaultAdminEmail)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .role("ADMIN")
                .enabled(true)
                .build();

        adminRepository.save(admin);
        log.warn("Bootstrapped default admin account for {}", defaultAdminEmail);
    }
}
