package com.rdc.admin.service;

import com.rdc.admin.repository.AdminRepository;
import com.rdc.admin.entity.Admin;
import com.rdc.admin.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Admin findById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found with id: " + id));
    }

    @Transactional
    public void updateAdminPassword(Long adminId, String rawNewPassword) {
        log.info("Updating password for Admin ID: {}", adminId);
        Admin admin = findById(adminId);

        // Ensure the new password is encrypted using the BCrypt encoder
        admin.setPassword(passwordEncoder.encode(rawNewPassword));
        adminRepository.save(admin);
    }
}