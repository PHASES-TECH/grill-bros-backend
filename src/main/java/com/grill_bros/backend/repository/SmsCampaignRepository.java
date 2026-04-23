package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.SmsCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SmsCampaignRepository extends JpaRepository<SmsCampaign, UUID> {

    List<SmsCampaign> findBySenderAdminIdOrderByCreatedAtDesc(UUID adminId);
}
