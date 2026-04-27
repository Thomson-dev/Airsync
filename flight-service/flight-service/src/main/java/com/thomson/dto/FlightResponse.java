package com.thomson.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Data returned to the client after a flight operation.
 * Excludes internal fields like totalSeats — only exposes what the client needs.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FlightResponse {
    private Long id;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Double price;
    private Integer availableSeats;
    /** String representation of FlightStatus e.g. "SCHEDULED" */
    private String status;
}
