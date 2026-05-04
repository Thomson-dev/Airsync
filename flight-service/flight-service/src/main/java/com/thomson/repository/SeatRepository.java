package com.thomson.repository;

import com.thomson.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightIdAndAvailable(Long flightId, Boolean available);

    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);
}
