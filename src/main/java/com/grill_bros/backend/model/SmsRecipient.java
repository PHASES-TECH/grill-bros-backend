package com.grill_bros.backend.model;

import com.grill_bros.backend.records.SmsStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sms_recipients")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRecipient extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private SmsCampaign campaign;

    private UUID recipientId;
    private String recipientName;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private SmsStatus status;

    private String externalMessageId; // ID from SMS provider
    private String errorMessage;

    private Instant sentAt;

}
