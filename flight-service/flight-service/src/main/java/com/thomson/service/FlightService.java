package com.thomson.service;

import com.thomson.dto.*;
import com.thomson.entity.*;
import com.thomson.exception.FlightNotFoundException;
import com.thomson.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;

    // @Transactional ensures flight + seats are saved together — if seat generation fails, the flight is rolled back too
    @Transactional
    public FlightResponse addFlight(FlightRequest request) {
        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureDate(request.getDepartureDate())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .price(request.getPrice())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .status(FlightStatus.SCHEDULED)
                .build();

        Flight saved = flightRepository.save(flight);
        generateSeats(saved);
        return mapToResponse(saved);
    }

    public List<FlightResponse> searchFlights(String origin, String destination, LocalDate date) {
        return flightRepository
                .findByOriginAndDestinationAndDepartureDate(origin, destination, date)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
        return mapToResponse(flight);
    }

    public List<SeatResponse> getAvailableSeats(Long flightId) {
        return seatRepository.findByFlightIdAndAvailable(flightId, true)
                .stream()
                .map(seat -> SeatResponse.builder()
                        .id(seat.getId())
                        .seatNumber(seat.getSeatNumber())
                        .seatClass(seat.getSeatClass().name())
                        .available(seat.getAvailable())
                        .build())
                .collect(Collectors.toList());
    }

    // Sets status to CANCELLED instead of deleting the record
    public String deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));
        flight.setStatus(FlightStatus.CANCELLED);
        flightRepository.save(flight);
        return "Flight cancelled successfully";
    }

    // Builds all seats in memory first, then saves in one batch instead of one DB call per seat
    private void generateSeats(Flight flight) {
        String[] cols = {"A", "B", "C", "D", "E", "F"};
        List<Seat> seats = new ArrayList<>();

        int totalSeats = flight.getTotalSeats();
        int seatCount = 0;

        for (int i = 1; seatCount < totalSeats; i++) {
            for (String col : cols) {
                if (seatCount >= totalSeats) break;

                seats.add(Seat.builder()
                        .seatNumber(i + col)
                        .seatClass(i <= 2 ? SeatClass.BUSINESS : SeatClass.ECONOMY)
                        .available(true)
                        .flight(flight)
                        .build());

                seatCount++;
            }
        }
        seatRepository.saveAll(seats);
    }


    

    private FlightResponse mapToResponse(Flight flight) {
        return FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureDate(flight.getDepartureDate())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .price(flight.getPrice())
                .availableSeats(flight.getAvailableSeats())
                .status(flight.getStatus().name())
                .build();
    }
}
