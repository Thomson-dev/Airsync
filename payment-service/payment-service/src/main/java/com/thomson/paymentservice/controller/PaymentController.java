package com.thomson.paymentservice.controller;

import com.thomson.paymentservice.dto.PaymentRequest;
import com.thomson.paymentservice.dto.PaymentResponse;
import com.thomson.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @PostMapping("/verify/{reference}")
    public ResponseEntity<PaymentResponse> verifyPayment(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.verifyPayment(reference));
    }
}
