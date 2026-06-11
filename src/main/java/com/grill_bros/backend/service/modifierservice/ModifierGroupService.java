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

    public List<ModifierGroupResponse> getAllGroupsNoPagination() {
        return groupRepo.findAll().stream().map(ModifierGroupResponse::from).toList();
    }

    @Transactional
    public ModifierGroupResponse createGroup(CreateModifierGroupRequest req) {

        List<MenuItem> menuItems = req.getMenuItemIds()
                .stream()
                .map(menuItemId -> menuItemRepo.findById(menuItemId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "MenuItem with ID " + menuItemId + " not found")))
                .toList();

        ModifierGroup group = new ModifierGroup();
        group.setName(req.getName());
        group.setMenuItems(menuItems);
        group.setRequired(req.isRequired());
        group.setMinSelections(req.getMinSelections());
        group.setMaxSelections(req.getMaxSelections());

        for (MenuItem menuItem : menuItems) {
            menuItem.getModifierGroups().add(group);
            menuItemRepo.save(menuItem);
        }

        groupRepo.save(group);
        return ModifierGroupResponse.from(group);
    }

    public List<ModifierGroupResponse> getModifierGroupsForMenuItem(UUID menuItemId) {
        return groupRepo.findByMenuItems_Id(menuItemId)
                .stream()
                .map(ModifierGroupResponse::from)
                .toList();
    }
}
