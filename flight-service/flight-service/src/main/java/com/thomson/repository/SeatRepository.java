package com.thomson.repository;

import com.thomson.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Data access layer for Seat entities.
 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Returns seats for a flight filtered by availability.
     * Pass available=true to get open seats, false for booked seats.
     */
    List<Seat> findByFlightIdAndAvailable(Long flightId, Boolean available);
}
