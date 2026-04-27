package com.thomson.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload received from the client when creating a new flight.
 */
@Data
public class FlightRequest {
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private Double price;
    /** Must be a multiple of 6 — seats are generated across columns A-F */
    private Integer totalSeats;
}
