package com.thomson.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {
    @NotNull
    private Long bookingId;
    @NotNull
    private Long userId;
    @NotNull
    private Double amount;
    @NotBlank
    private String email;
}
