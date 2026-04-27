package com.thomson.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents an individual seat on a flight.
 * Mapped to the "seats" table in the database.
 */
@Entity
@Table(name = "seats")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Seat {

    /** Auto-incremented primary key */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Seat label e.g. "1A", "3C" */
    private String seatNumber;

    /** ECONOMY or BUSINESS — stored as string in the DB */
    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    /** True if seat has not been booked yet */
    private Boolean available;

    /** The flight this seat belongs to */
    @ManyToOne
    @JoinColumn(name = "flight_id")
    private Flight flight;
}
