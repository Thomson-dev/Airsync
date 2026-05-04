package com.thomson.bookingservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

// name must match spring.application.name of flight-service
@FeignClient(name = "flight-service")
public interface FlightServiceClient {

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
