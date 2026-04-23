package com.grill_bros.backend.model;

import com.grill_bros.backend.records.CampaignStatus;
import com.grill_bros.backend.records.RecipientType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sms_campaigns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsCampaign extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_admin_id", nullable = false)
    private Users senderAdmin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientType recipientType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    private Integer totalRecipients;
    private Integer sentCount = 0;
    private Integer failedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    private Instant startedAt;
    private Instant completedAt;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL)
    private List<SmsRecipient> recipients = new ArrayList<>();

}
