package com.thomson.paymentservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

@FeignClient(name = "booking-service")
public interface BookingServiceClient {

    @PutMapping("/api/bookings/{bookingId}/confirm")
    void confirmBooking(@PathVariable Long bookingId);
}
