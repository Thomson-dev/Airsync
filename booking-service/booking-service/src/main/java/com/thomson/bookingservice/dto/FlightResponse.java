package com.thomson.bookingservice.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlightResponse {
    private Long id;
    private Double price;
}
