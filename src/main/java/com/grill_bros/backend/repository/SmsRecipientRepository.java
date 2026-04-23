package com.grill_bros.backend.repository;

import com.grill_bros.backend.model.SmsRecipient;
import com.grill_bros.backend.records.SmsStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SmsRecipientRepository extends JpaRepository<SmsRecipient, UUID> {

    List<SmsRecipient> findByCampaignIdAndStatus(UUID campaignId, SmsStatus status);
}
