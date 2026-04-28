package com.grill_bros.backend.service.modifierservice;

import com.grill_bros.backend.dto.modifierdtos.CreateModifierGroupRequest;
import com.grill_bros.backend.dto.modifierdtos.ModifierGroupResponse;
import com.grill_bros.backend.dto.modifierdtos.ModifierResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.MenuItem;
import com.grill_bros.backend.model.ModifierGroup;
import com.grill_bros.backend.repository.MenuItemRepository;
import com.grill_bros.backend.repository.ModifierGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModifierGroupService {

    private final ModifierGroupRepository groupRepo;
    private final MenuItemRepository menuItemRepo;

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
}
