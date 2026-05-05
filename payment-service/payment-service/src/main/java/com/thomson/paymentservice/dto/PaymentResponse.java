package com.thomson.paymentservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private String reference;
    private String authorizationUrl;
    private String status;
}
