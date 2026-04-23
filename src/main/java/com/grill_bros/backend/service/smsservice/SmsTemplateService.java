package com.grill_bros.backend.service.smsservice;//package com.catholic_church.backend.service.smsservice;
//
//import com.catholic_church.backend.dto.SmsTemplateRequest;
//import com.catholic_church.backend.dto.UsersDto;
//import com.catholic_church.backend.exceptions.ResourceNotFoundException;
//import com.catholic_church.backend.model.SmsTemplate;
//import com.catholic_church.backend.model.Users;
//import com.catholic_church.backend.repository.SmsTemplateRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Optional;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//public class SmsTemplateService {
//
//    private final SmsTemplateRepository smsTemplateRepository;
//
//    public SmsTemplate toEntity(SmsTemplateRequest dto) {
//        SmsTemplate template = new SmsTemplate();
//        template.setCode(dto.getCode());
//        template.setSender(dto.getSender());
//        template.setContent(dto.getContent());
//        return template;
//    }
//
//    public void createTemplate(SmsTemplateRequest request) {
//        SmsTemplate template = toEntity(request);
//        smsTemplateRepository.save(template);
//    }
//
//    public SmsTemplate getTemplateByCode(String code) {
//        return smsTemplateRepository.findByCode(code)
//                .orElseThrow(() ->
//                        new RuntimeException("SMS template not found: " + code));
//    }
//
//    @Transactional
//    public SmsTemplate updateTemplate(UUID id, SmsTemplateRequest request) {
//
//        SmsTemplate template = smsTemplateRepository.findById(id)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException("Template not found"));
//
//        template.setCode(request.getCode());
//        template.setSender(request.getSender());
//        template.setContent(request.getContent());
//        template.setOtp(request.isOtp());
//
//        return smsTemplateRepository.save(template);
//    }
//
//}
