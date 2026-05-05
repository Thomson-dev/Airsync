package com.thomson.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    @NotNull
    private Long userId;
    @NotNull
    private Long flightId;
    @NotBlank
    private String seatNumber;
    @NotNull
    @Positive
    private Double totalPrice;
}
