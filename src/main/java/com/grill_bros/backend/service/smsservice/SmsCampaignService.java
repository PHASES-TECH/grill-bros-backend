package com.grill_bros.backend.service.smsservice;

import com.grill_bros.backend.dto.bulksms.BulkSmsRequest;
import com.grill_bros.backend.dto.bulksms.SmsCampaignResponse;
import com.grill_bros.backend.dto.bulksms.SmsResponse;
import com.grill_bros.backend.model.Customer;
import com.grill_bros.backend.model.SmsCampaign;
import com.grill_bros.backend.model.SmsRecipient;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.CampaignStatus;
import com.grill_bros.backend.records.RecipientType;
import com.grill_bros.backend.records.SmsStatus;
import com.grill_bros.backend.repository.CustomerRepository;
import com.grill_bros.backend.repository.SmsCampaignRepository;
import com.grill_bros.backend.repository.SmsRecipientRepository;
import com.grill_bros.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsCampaignService {

    private final SmsCampaignRepository campaignRepository;
    private final SmsRecipientRepository recipientRepository;
    private final SmsRecipientService smsRecipientService;
    private final SmsProviderService smsProviderService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public SmsCampaignResponse createAndSendCampaign(Users currentAdmin, BulkSmsRequest request) {
        // Create campaign
        SmsCampaign campaign = SmsCampaign.builder()
                .senderAdmin(currentAdmin)
                .recipientType(request.getRecipientType())
                .message(request.getMessage())
                .status(CampaignStatus.PENDING)
                .build();

        // Get recipient contacts
        List<String> contacts = smsRecipientService.getRecipientContacts(currentAdmin, request);
        campaign.setTotalRecipients(contacts.size());

        // Create recipient records
        List<SmsRecipient> recipients = createRecipientRecords(campaign, contacts, request);
        campaign.setRecipients(recipients);

        // Save campaign
        SmsCampaign savedCampaign = campaignRepository.save(campaign);

        // Send SMS asynchronously
        sendCampaignAsync(savedCampaign.getId());

        return mapToResponse(savedCampaign);
    }

    private List<SmsRecipient> createRecipientRecords(
            SmsCampaign campaign,
            List<String> contacts,
            BulkSmsRequest request) {

        List<SmsRecipient> recipients = new ArrayList<>();

        // Get member/admin details for better tracking
        if (request.getRecipientType() == RecipientType.CUSTOMERS) {
            List<Customer> customers = getCustomersFromContacts(contacts);
            for (Customer customer : customers) {
                recipients.add(SmsRecipient.builder()
                        .campaign(campaign)
                        .recipientId(customer.getId())
                        .recipientName(customer.getFullName())
                        .phoneNumber(customer.getPhoneNumber())
                        .status(SmsStatus.PENDING)
                        .build());
            }
        } else {
            List<Users> admins = getAdminsFromContacts(contacts);
            for (Users admin : admins) {
                recipients.add(SmsRecipient.builder()
                        .campaign(campaign)
                        .recipientId(admin.getId())
                        .recipientName(admin.getFullName())
                        .phoneNumber(admin.getPhoneNumber())
                        .status(SmsStatus.PENDING)
                        .build());
            }
        }

        return recipients;
    }

    @Async
    @Transactional
    public void sendCampaignAsync(UUID campaignId) {
        SmsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        campaign.setStatus(CampaignStatus.PROCESSING);
        campaign.setStartedAt(Instant.now());
        campaignRepository.save(campaign);

        log.info("Starting SMS campaign {} with {} recipients", campaignId, campaign.getTotalRecipients());

        int successCount = 0;
        int failCount = 0;

        // Get pending recipients
        List<SmsRecipient> pendingRecipients = recipientRepository
                .findByCampaignIdAndStatus(campaignId, SmsStatus.PENDING);

        List<String> phoneNumbers = pendingRecipients.stream()
                .map(SmsRecipient::getPhoneNumber)
                .collect(Collectors.toList());

        try {
            // Send one request with all numbers instead of looping
            SmsResponse response = smsProviderService.sendSms(phoneNumbers, campaign.getMessage());

            if (response.isSuccess()) {
                // Mark all recipients as sent in one go
                pendingRecipients.forEach(recipient -> {
                    recipient.setStatus(SmsStatus.SENT);
                    recipient.setExternalMessageId(response.getMessageId());
                    recipient.setSentAt(Instant.now());
                });
                successCount += pendingRecipients.size();
            } else {
                // Mark all as failed
                pendingRecipients.forEach(recipient -> {
                    recipient.setStatus(SmsStatus.FAILED);
                    recipient.setErrorMessage(response.getErrorMessage());
                });
                failCount += pendingRecipients.size();
            }

            // Save all recipients in one database call instead of one by one
            recipientRepository.saveAll(pendingRecipients);

        } catch (Exception e) {
            log.error("Failed to send bulk SMS for campaign {}: {}", campaign.getId(), e.getMessage());
            pendingRecipients.forEach(recipient -> {
                recipient.setStatus(SmsStatus.FAILED);
                recipient.setErrorMessage(e.getMessage());
            });
            recipientRepository.saveAll(pendingRecipients);
            failCount += pendingRecipients.size();
        }



        // Update campaign status
        campaign.setSentCount(successCount);
        campaign.setFailedCount(failCount);
        campaign.setStatus(failCount == 0 ? CampaignStatus.COMPLETED : CampaignStatus.FAILED);
        campaign.setCompletedAt(Instant.now());
        campaignRepository.save(campaign);

        log.info("Completed SMS campaign {}. Sent: {}, Failed: {}",
                campaignId, successCount, failCount);
    }

    public List<SmsCampaignResponse> getCampaignHistory(Users admin) {
        List<SmsCampaign> campaigns = campaignRepository
                .findBySenderAdminIdOrderByCreatedAtDesc(admin.getId());

        return campaigns.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public void retryFailedMessages(UUID campaignId) {
        List<SmsRecipient> failedRecipients = recipientRepository
                .findByCampaignIdAndStatus(campaignId, SmsStatus.FAILED);

        for (SmsRecipient recipient : failedRecipients) {
            recipient.setStatus(SmsStatus.PENDING);
            recipient.setErrorMessage(null);
            recipientRepository.save(recipient);
        }

        sendCampaignAsync(campaignId);
    }

    private List<Customer> getCustomersFromContacts(List<String> contacts) {
        // Implementation to fetch members - can be optimized with IN query
        return customerRepository.findAll().stream()
                .filter(c -> contacts.contains(c.getPhoneNumber()))
                .toList();
    }

    private List<Users> getAdminsFromContacts(List<String> contacts) {
        return userRepository.findAll().stream()
                .filter(u -> contacts.contains(u.getPhoneNumber()))
                .toList();
    }

    private SmsCampaignResponse mapToResponse(SmsCampaign campaign) {
        SmsCampaignResponse response = new SmsCampaignResponse();
        response.setId(campaign.getId());
        response.setStatus(campaign.getStatus().toString());
        response.setTotalRecipients(campaign.getTotalRecipients());
        response.setSentCount(campaign.getSentCount());
        response.setFailedCount(campaign.getFailedCount());
        response.setMessage(campaign.getMessage());
        return response;
    }
}
