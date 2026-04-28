package com.grill_bros.backend.controllers;

import com.grill_bros.backend.dto.modifierdtos.BulkCreateModifiersRequest;
import com.grill_bros.backend.dto.modifierdtos.CreateModifierGroupRequest;
import com.grill_bros.backend.dto.modifierdtos.CreateModifierRequest;
import com.grill_bros.backend.service.modifierservice.ModifierGroupService;
import com.grill_bros.backend.service.modifierservice.ModifierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/modifiers")
@RequiredArgsConstructor
public class ModifiersController {

    private final ModifierGroupService modifierGroupService;
    private final ModifierService modifierService;

    @PostMapping("/group")
    public ResponseEntity<?> createGroup(@Valid @RequestBody CreateModifierGroupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierGroupService.createGroup(req));
    }

    @PostMapping
    public ResponseEntity<?> createModifier(@Valid @RequestBody CreateModifierRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierService.createModifier(req));
    }

    // 🔥 Bulk create
    @PostMapping("/bulk")
    public ResponseEntity<?> createBulk(@Valid @RequestBody BulkCreateModifiersRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(modifierService.createBulk(req));
    }
}
