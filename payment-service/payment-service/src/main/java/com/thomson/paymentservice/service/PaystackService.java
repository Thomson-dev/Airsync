package com.thomson.paymentservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class PaystackService {

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> initializeTransaction(String email, double amountNaira) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "email", email,
                "amount", (long) (amountNaira * 100) // convert to kobo
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/transaction/initialize", request, Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return data;
    }

    public boolean verifyTransaction(String reference) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/transaction/verify/" + reference,
                HttpMethod.GET, request, Map.class);

        Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
        return "success".equals(data.get("status"));
    }
}
