package com.thomson.notificationservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private Long paymentId;
    private Long bookingId;
    private Long userId;
    private Double amount;
    private String email;
    private String eventType;
}
