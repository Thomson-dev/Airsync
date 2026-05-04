package com.thomson.bookingservice.service;

import com.thomson.bookingservice.dto.BookingRequest;
import com.thomson.bookingservice.dto.BookingResponse;
import com.thomson.bookingservice.entity.Booking;
import com.thomson.bookingservice.entity.BookingStatus;
import com.thomson.bookingservice.exception.BookingNotFoundException;
import com.thomson.bookingservice.feign.FlightServiceClient;
import com.thomson.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightServiceClient flightServiceClient;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        flightServiceClient.bookSeat(request.getFlightId(), request.getSeatNumber());

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .flightId(request.getFlightId())
                .seatNumber(request.getSeatNumber())
                .totalPrice(request.getTotalPrice())
                .status(BookingStatus.CONFIRMED)
                .bookingTime(LocalDateTime.now())
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking " + bookingId + " is already cancelled");
        }

        flightServiceClient.releaseSeat(booking.getFlightId(), booking.getSeatNumber());

        booking.setStatus(BookingStatus.CANCELLED);
        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse getBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    public List<BookingResponse> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUserId())
                .flightId(booking.getFlightId())
                .seatNumber(booking.getSeatNumber())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .bookingTime(booking.getBookingTime())
                .build();
    }
}
