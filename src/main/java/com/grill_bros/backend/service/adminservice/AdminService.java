package com.grill_bros.backend.service.adminservice;

import com.grill_bros.backend.dto.AdminRequestDto;
import com.grill_bros.backend.dto.AdminUserResponse;
import com.grill_bros.backend.dto.UpdateAdminUserRequest;
import com.grill_bros.backend.exceptions.DuplicateResourceException;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository adminUserRepository;
    private final PasswordEncoder     passwordEncoder;

    // ── List ──────────────────────────────────────────────────────────────────

    public Page<AdminUserResponse> listAdmins(Pageable pageable) {
        return adminUserRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminUserResponse::from);
    }

    public AdminUserResponse getById(UUID id) {
        return adminUserRepository.findById(id)
                .map(AdminUserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public AdminUserResponse create(AdminRequestDto req) {
        if (adminUserRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException(
                    "Admin with email already exists: " + req.getEmail());
        }
        Users admin = Users.create(
                req.getEmail(),
                passwordEncoder.encode(req.getPassword()),
                req.getFullName(),
                req.getRole());

        Users saved = adminUserRepository.save(admin);
        log.info("Admin user created: {} with role: {}", saved.getEmail(), saved.getRole());
        return AdminUserResponse.from(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public AdminUserResponse update(UUID id, UpdateAdminUserRequest req) {
        Users admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));

        admin.setFullName(req.getFullName());
        admin.setRole(req.getRole());

        return AdminUserResponse.from(adminUserRepository.save(admin));
    }

    // ── Activate / Deactivate ─────────────────────────────────────────────────

    @Transactional
    public void deactivate(UUID id) {
        Users admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));
        admin.deactivate();
        adminUserRepository.save(admin);
        log.info("Admin user deactivated: {}", admin.getEmail());
    }

    @Transactional
    public void activate(UUID id) {
        Users admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));
        admin.activate();
        adminUserRepository.save(admin);
    }

    // ── Hard delete (SUPER_ADMIN only, enforced at controller level) ──────────

    @Transactional
    public void delete(UUID id) {
        Users admin = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminUser"));
        adminUserRepository.delete(admin);
        log.warn("Admin user permanently deleted: {}", admin.getEmail());
    }
}
