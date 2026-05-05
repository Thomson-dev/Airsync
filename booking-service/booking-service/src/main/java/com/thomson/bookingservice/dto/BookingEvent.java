package com.thomson.bookingservice.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingEvent {
    private Long bookingId;
    private Long userId;
    private Long flightId;
    private String seatNumber;
    private Double totalPrice;
    private String eventType; // BOOKING_CONFIRMED, BOOKING_CANCELLED
}
