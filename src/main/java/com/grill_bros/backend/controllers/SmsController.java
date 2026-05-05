package com.grill_bros.backend.controllers;

import com.grill_bros.backend.common.ApiResponse;
import com.grill_bros.backend.dto.bulksms.BulkSmsPreviewResponse;
import com.grill_bros.backend.dto.bulksms.BulkSmsRequest;
import com.grill_bros.backend.dto.bulksms.SmsCampaignResponse;
import com.grill_bros.backend.model.UserPrincipal;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.service.smsservice.SmsCampaignService;
import com.grill_bros.backend.service.smsservice.SmsRecipientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsRecipientService smsRecipientService;
    private final SmsCampaignService smsCampaignService;

    @PostMapping("/send")
    public ResponseEntity<SmsCampaignResponse> sendBulkSms(
            @RequestBody BulkSmsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Users currentAdmin = principal.getUser();
        SmsCampaignResponse campaign = smsCampaignService.createAndSendCampaign(currentAdmin, request);

        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<SmsCampaignResponse>> getCampaignHistory(
            @AuthenticationPrincipal UserPrincipal principal) {

        Users currentAdmin = principal.getUser();
        List<SmsCampaignResponse> campaigns = smsCampaignService.getCampaignHistory(currentAdmin);

        return ResponseEntity.ok(campaigns);
    }

    @PostMapping("/campaigns/{campaignId}/retry")
    public ResponseEntity<Void> retryFailedMessages(
            @PathVariable UUID campaignId,
            @AuthenticationPrincipal UserPrincipal principal) {

        smsCampaignService.retryFailedMessages(campaignId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/preview")
    public ResponseEntity<?> previewRecipients(
            @RequestBody BulkSmsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Users currentAdmin = principal.getUser();

        List<String> contacts = smsRecipientService.getRecipientContacts(currentAdmin, request);

        BulkSmsPreviewResponse response = new BulkSmsPreviewResponse();
        response.setTotalRecipients(contacts.size());

        return ResponseEntity.ok(ApiResponse.ok(contacts,"Contacts fetched"));
    }
}
