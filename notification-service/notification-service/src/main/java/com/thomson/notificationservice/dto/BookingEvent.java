package com.thomson.notificationservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingEvent {
    private Long bookingId;
    private Long userId;
    private Long flightId;
    private String seatNumber;
    private Double totalPrice;
    private String eventType;
}
