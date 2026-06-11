package com.grill_bros.backend.service.modifierservice;

import com.grill_bros.backend.dto.modifierdtos.BulkCreateModifiersRequest;
import com.grill_bros.backend.dto.modifierdtos.CreateModifierRequest;
import com.grill_bros.backend.dto.modifierdtos.ModifierResponse;
import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Modifier;
import com.grill_bros.backend.model.ModifierGroup;
import com.grill_bros.backend.repository.ModifierGroupRepository;
import com.grill_bros.backend.repository.ModifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModifierService {

    private final ModifierRepository modifierRepo;
    private final ModifierGroupRepository groupRepo;

    public Page<ModifierResponse> getAllModifiers(Pageable pageable) {
        return modifierRepo.findAll(pageable)
                .map(modifier -> ModifierResponse.builder()
                        .id(modifier.getId())
                        .name(modifier.getName())
                        .price(modifier.getPrice())
                        .build()
                );
    }

    @Transactional
    public ModifierResponse createModifier(CreateModifierRequest req) {

        ModifierGroup group = groupRepo.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup"));

        BigDecimal price = new BigDecimal(req.getPrice());

        Modifier modifier = new Modifier();
        modifier.setName(req.getName());
        modifier.setPrice(price);
        modifier.setGroup(group);

        modifierRepo.save(modifier);

        return ModifierResponse.from(modifier);
    }

    public List<ModifierResponse> getModifiersForMenuItem(UUID menuItemId) {
        return groupRepo
                .findByMenuItems_Id(menuItemId)
                .stream()
                .flatMap(group -> group.getModifiers().stream())
                .map(modifier -> ModifierResponse.builder()
                        .id(modifier.getId())
                        .name(modifier.getName())
                        .price(modifier.getPrice())
                        .build())
                .toList();
    }

    @Transactional
    public List<ModifierResponse> createBulk(BulkCreateModifiersRequest req) {

        ModifierGroup group = groupRepo.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup"));

        List<Modifier> modifiers = req.getModifiers().stream()
                .map(m -> {
                    Modifier mod = new Modifier();
                    BigDecimal price = new BigDecimal(m.getPrice());
                    mod.setName(m.getName());
                    mod.setPrice(price);
                    mod.setGroup(group);
                    return mod;
                })
                .toList();

        modifierRepo.saveAll(modifiers);

        return modifiers.stream()
                .map(ModifierResponse::from)
                .toList();
    }
}
