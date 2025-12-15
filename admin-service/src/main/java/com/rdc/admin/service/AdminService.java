package com.rdc.admin.service;

import com.rdc.admin.repository.AdminRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    // This service would primarily house business logic related to Admin profiles,
    // such as changing passwords, checking permissions, or tracking activity.

    /**
     * Placeholder method for finding an Admin by ID (e.g., used when tracking who uploaded a design).
     */
    // public AdminDto findById(Long id) {
    //    // return adminRepository.findById(id).map(this::mapToDto)...
    //    return null;
    // }
}