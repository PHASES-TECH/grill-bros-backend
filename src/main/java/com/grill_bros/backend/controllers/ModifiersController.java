package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.common.PagedResponse;
import com.grill_bros.backend.dto.menudtos.MenuItemResponse;
import com.grill_bros.backend.dto.modifierdtos.*;
import com.grill_bros.backend.service.modifierservice.ModifierGroupService;
import com.grill_bros.backend.service.modifierservice.ModifierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/modifiers")
@RequiredArgsConstructor
public class ModifiersController {

    private final ModifierGroupService modifierGroupService;
    private final ModifierService modifierService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ModifierResponse>>> getAllModifiers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<ModifierResponse> result = modifierService.getAllModifiers(pageable);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(result)));
    }

    @GetMapping("/admin/groups")
    public ResponseEntity<ApiResponse<List<ModifierGroupResponse>>> getAllModifierGroupsNoPagination(
    ) {
        List<ModifierGroupResponse> result = modifierGroupService.getAllGroupsNoPagination();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }


    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<PagedResponse<ModifierGroupResponse>>> getAllModifierGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        size = Math.min(size, 100);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        Page<ModifierGroupResponse> result = modifierGroupService.getAllGroups(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(result)));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createGroup(@Valid @RequestBody CreateModifierGroupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierGroupService.createGroup(req));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createModifier(@Valid @RequestBody CreateModifierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierService.createModifier(req));
    }

    // 🔥 Bulk create
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createBulk(@Valid @RequestBody BulkCreateModifiersRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierService.createBulk(req));
    }
}
