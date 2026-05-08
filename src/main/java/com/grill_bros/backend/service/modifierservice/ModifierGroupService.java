package com.grill_bros.backend.service.modifierservice;

import com.grill_bros.backend.dto.modifierdtos.CreateModifierGroupRequest;
import com.grill_bros.backend.dto.modifierdtos.ModifierGroupResponse;
import com.grill_bros.backend.dto.modifierdtos.ModifierResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.MenuItem;
import com.grill_bros.backend.model.Modifier;
import com.grill_bros.backend.model.ModifierGroup;
import com.grill_bros.backend.repository.MenuItemRepository;
import com.grill_bros.backend.repository.ModifierGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModifierGroupService {

    private final ModifierGroupRepository groupRepo;
    private final MenuItemRepository menuItemRepo;

    public Page<ModifierGroupResponse> getAllGroups(Pageable pageable) {
        return groupRepo.findAll(pageable)
                .map(ModifierGroupResponse::from);
    }

    @Transactional
    public ModifierGroupResponse createGroup(CreateModifierGroupRequest req) {

        MenuItem menuItem = menuItemRepo.findById(req.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem"));

        ModifierGroup group = new ModifierGroup();
        group.setName(req.getName());
        group.setMenuItem(menuItem);
        group.setRequired(req.isRequired());
        group.setMinSelections(req.getMinSelections());
        group.setMaxSelections(req.getMaxSelections());

        groupRepo.save(group);
        return ModifierGroupResponse.from(group);
    }

    public List<ModifierResponse> getModifierGroupsForMenuItem(UUID menuItemId) {
        List<ModifierGroup> modifierGroups = groupRepo.findByMenuItemId(menuItemId);

        if (modifierGroups == null || modifierGroups.isEmpty()) {
            return Collections.emptyList();
        }

        List<ModifierResponse> responseList = new ArrayList<>();

        for (ModifierGroup group : modifierGroups) {
            List<Modifier> modifiers = group.getModifiers();

            if (modifiers == null || modifiers.isEmpty()) continue;

            for (Modifier modifier : modifiers) {
                ModifierResponse response = ModifierResponse.builder()
                        .id(modifier.getId())
                        .name(modifier.getName())
                        .price(modifier.getPrice())
                        .groupId(group.getId())
                        .groupName(group.getName())
                        .build();

                responseList.add(response);
            }
        }

        return responseList;
    }
}
