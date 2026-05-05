package com.thomson.bookingservice.feign;

import com.thomson.bookingservice.dto.FlightResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "flight-service")
public interface FlightServiceClient {

    @GetMapping("/api/flights/{flightId}")
    FlightResponse getFlight(@PathVariable Long flightId);

    @PutMapping("/api/flights/{flightId}/seats/{seatNumber}/book")
    void bookSeat(
        @PathVariable Long flightId,
        @PathVariable String seatNumber
    );

    @PutMapping("/api/flights/{flightId}/seats/{seatNumber}/release")
    void releaseSeat(
        @PathVariable Long flightId,
        @PathVariable String seatNumber
    );
}
