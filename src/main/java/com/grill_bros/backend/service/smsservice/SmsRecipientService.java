package com.grill_bros.backend.service.smsservice;

import com.grill_bros.backend.dto.bulksms.BulkSmsRequest;
import com.grill_bros.backend.model.Customer;
import com.grill_bros.backend.model.Users;
import com.grill_bros.backend.records.RecipientType;
import com.grill_bros.backend.repository.CustomerRepository;
import com.grill_bros.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsRecipientService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public List<String> getRecipientContacts(Users currentAdmin, BulkSmsRequest request) {

        if (request.getRecipientType() == RecipientType.CUSTOMERS) {
            return getCustomerContacts(currentAdmin, request);
        } else {
            return getAdminContacts(currentAdmin, request);
        }
    }

    private List<String> getCustomerContacts(Users currentAdmin, BulkSmsRequest request) {
        List<Customer> customers = new ArrayList<>();

        customers = customerRepository.findAll();

        return customers.stream()
                .map(Customer::getPhoneNumber)
                .filter(contact -> contact != null && !contact.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getAdminContacts(Users currentAdmin, BulkSmsRequest request) {
        List<Users> admins = new ArrayList<>();

        admins = userRepository.findAll();

        return admins.stream()
                .map(Users::getPhoneNumber)
                .filter(phone -> phone != null && !phone.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
}
