package com.thomson.bookingservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingRequest {
    private Long userId;
    private Long flightId;
    private String seatNumber;
    private Double totalPrice;
}
