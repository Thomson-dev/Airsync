package com.thomson.repository;

import com.thomson.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access layer for Flight entities.
 * Spring Data generates the query from the method name at runtime.
 */
public interface FlightRepository extends JpaRepository<Flight, Long> {

    /**
     * Finds all flights matching the given route and date.
     * Used by the flight search endpoint.
     */
    List<Flight> findByOriginAndDestinationAndDepartureDate(
        String origin,
        String destination,
        LocalDate departureDate
    );
}
