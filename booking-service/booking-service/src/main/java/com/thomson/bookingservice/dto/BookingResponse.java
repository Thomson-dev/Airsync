package com.thomson.bookingservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingResponse {
    private Long id;
    private Long userId;
    private Long flightId;
    private String seatNumber;
    private Double totalPrice;
    private String status;
    private LocalDateTime bookingTime;
}
