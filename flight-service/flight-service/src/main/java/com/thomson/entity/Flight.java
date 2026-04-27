package com.thomson.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a flight in the system.
 * Mapped to the "flights" table in the database.
 */
@Entity
@Table(name = "flights")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Flight {

    /** Auto-incremented primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique flight identifier e.g. "NG101" */
    private String flightNumber;

    private String origin;
    private String destination;

    private LocalDate departureDate;
    private LocalTime departureTime;
    private LocalTime arrivalTime;

    /** Ticket price in local currency */
    private Double price;

    /** Total number of seats on the flight */
    private Integer totalSeats;

    /** Tracks how many seats are still open for booking */
    private Integer availableSeats;

    /** Stored as a string in the DB (SCHEDULED, CANCELLED, COMPLETED) */
    @Enumerated(EnumType.STRING)
    private FlightStatus status;
}
