package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModifierRepository extends JpaRepository<Modifier, UUID> {
}
