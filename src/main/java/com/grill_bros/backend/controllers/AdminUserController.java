package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.AdminRequestDto;
import com.grill_bros.backend.dto.AdminUserResponse;
import com.grill_bros.backend.dto.UpdateAdminUserRequest;
import com.grill_bros.backend.service.adminservice.AdminService;
import com.grill_bros.backend.service.userservice.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin user CRUD — SUPER_ADMIN only.
 *
 * Double-protected: SecurityConfig restricts /admin/users/** to SUPER_ADMIN
 * AND @PreAuthorize at method level for defence-in-depth.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    @Operation(summary = "List all admin users (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<AdminUserResponse>>> listAdmins(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                ApiResponse.ok(PagedResponse.of(adminService.listAdmins(pageable))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a specific admin user by ID")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getAdmin(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new admin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAdmin(
            @Valid @RequestBody AdminRequestDto req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(adminService.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update admin user details or role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateAdmin(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAdminUserRequest req) {

        return ResponseEntity.ok(ApiResponse.ok(adminService.update(id, req)));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Disable an admin account (reversible)")
    public ResponseEntity<ApiResponse<Void>> deactivateAdmin(@PathVariable UUID id) {
        adminService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Admin account deactivated"));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Re-enable a deactivated admin account")
    public ResponseEntity<ApiResponse<Void>> activateAdmin(@PathVariable UUID id) {
        adminService.activate(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Admin account activated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete an admin user")
    public ResponseEntity<ApiResponse<Void>> deleteAdmin(@PathVariable UUID id) {
        adminService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Admin user deleted"));
    }
}
