package com.thomson.paymentservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private Double amount;
    private String email;
    private String eventType; // PAYMENT_SUCCESS, PAYMENT_FAILED
}
