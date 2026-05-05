package com.thomson.paymentservice.service;

import com.thomson.paymentservice.dto.PaymentEvent;
import com.thomson.paymentservice.dto.PaymentRequest;
import com.thomson.paymentservice.dto.PaymentResponse;
import com.thomson.paymentservice.entity.Payment;
import com.thomson.paymentservice.entity.PaymentStatus;
import com.thomson.paymentservice.feign.BookingServiceClient;
import com.thomson.paymentservice.publisher.PaymentEventPublisher;
import com.thomson.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaystackService paystackService;
    private final BookingServiceClient bookingServiceClient;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        Map<String, Object> paystackData = paystackService.initializeTransaction(
                request.getEmail(), request.getAmount());

        String reference = (String) paystackData.get("reference");
        String authorizationUrl = (String) paystackData.get("authorization_url");

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .email(request.getEmail())
                .reference(reference)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment initiated for booking {} with reference {}", request.getBookingId(), reference);

        return PaymentResponse.builder()
                .id(saved.getId())
                .bookingId(saved.getBookingId())
                .reference(reference)
                .authorizationUrl(authorizationUrl)
                .status(saved.getStatus().name())
                .build();
    }

    @Transactional
    public PaymentResponse verifyPayment(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found for reference: " + reference));

        boolean success = paystackService.verifyTransaction(reference);

        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            bookingServiceClient.confirmBooking(payment.getBookingId());
            log.info("Payment verified for booking {} — confirming booking", payment.getBookingId());

            eventPublisher.publish(PaymentEvent.builder()
                    .paymentId(payment.getId())
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .email(payment.getEmail())
                    .eventType("PAYMENT_SUCCESS")
                    .build());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.warn("Payment failed for booking {}", payment.getBookingId());

            eventPublisher.publish(PaymentEvent.builder()
                    .paymentId(payment.getId())
                    .bookingId(payment.getBookingId())
                    .userId(payment.getUserId())
                    .amount(payment.getAmount())
                    .email(payment.getEmail())
                    .eventType("PAYMENT_FAILED")
                    .build());
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .reference(payment.getReference())
                .status(payment.getStatus().name())
                .build();
    }
}
