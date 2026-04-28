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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModifierService {

    private final ModifierRepository modifierRepo;
    private final ModifierGroupRepository groupRepo;

    public ModifierResponse createModifier(CreateModifierRequest req) {

        ModifierGroup group = groupRepo.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup"));

        Modifier modifier = new Modifier();
        modifier.setName(req.getName());
        modifier.setPrice(req.getPrice());
        modifier.setGroup(group);

        modifierRepo.save(modifier);

        return ModifierResponse.from(modifier);
    }

    public List<ModifierResponse> createBulk(BulkCreateModifiersRequest req) {

        ModifierGroup group = groupRepo.findById(req.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("ModifierGroup"));

        List<Modifier> modifiers = req.getModifiers().stream()
                .map(m -> {
                    Modifier mod = new Modifier();
                    mod.setName(m.getName());
                    mod.setPrice(m.getPrice());
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
