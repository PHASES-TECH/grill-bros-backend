package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {
    List<ModifierGroup> findByMenuItems_Id(UUID menuItemId);
}
