package com.grill_bros.backend.service.smsservice;//package com.catholic_church.backend.service.smsservice;
//
//import com.catholic_church.backend.dto.MNotifySmsRequest;
//import com.catholic_church.backend.properties.MNotifyProperties;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//@Service
//@RequiredArgsConstructor
//public class MNotifySmsService {
//
//    private final MNotifyProperties mNotifyProperties;
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    public String sendSms(MNotifySmsRequest request) {
//
//        String url = mNotifyProperties.getBaseUrl() + "?key=" + mNotifyProperties.getApiKey();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//
//        HttpEntity<MNotifySmsRequest> entity =
//                new HttpEntity<>(request, headers);
//
//        ResponseEntity<String> response =
//                restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//
//        return response.getBody();
//    }
//}
