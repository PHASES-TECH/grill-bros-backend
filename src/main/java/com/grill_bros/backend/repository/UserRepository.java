package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {
    Users findByEmail(String email);

    Optional<Users> findUserByEmail(String email);

    Users findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    Page<Users> findAllByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Users> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // Role counts
    long countByRole(Role role);

    // Recent activity
    long countByCreatedAtAfter(Instant instant);
}

