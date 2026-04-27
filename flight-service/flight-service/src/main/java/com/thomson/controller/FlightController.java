package com.thomson.controller;

import com.thomson.dto.*;
import com.thomson.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @PostMapping
    public ResponseEntity<FlightResponse> addFlight(@RequestBody FlightRequest request) {
        return ResponseEntity.ok(flightService.addFlight(request));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FlightResponse>> searchFlights(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(flightService.searchFlights(from, to, date));
    }

    @GetMapping("/{flightId}")
    public ResponseEntity<FlightResponse> getFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(flightService.getFlightById(flightId));
    }

    @GetMapping("/{flightId}/seats")
    public ResponseEntity<List<SeatResponse>> getAvailableSeats(@PathVariable Long flightId) {
        return ResponseEntity.ok(flightService.getAvailableSeats(flightId));
    }

    @DeleteMapping("/{flightId}")
    public ResponseEntity<String> deleteFlight(@PathVariable Long flightId) {
        return ResponseEntity.ok(flightService.deleteFlight(flightId));
    }
}
