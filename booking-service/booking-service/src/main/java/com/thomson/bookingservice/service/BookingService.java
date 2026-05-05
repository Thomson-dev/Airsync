package com.thomson.bookingservice.service;

import com.thomson.bookingservice.dto.BookingEvent;
import com.thomson.bookingservice.dto.BookingRequest;
import com.thomson.bookingservice.dto.BookingResponse;
import com.thomson.bookingservice.entity.Booking;
import com.thomson.bookingservice.entity.BookingStatus;
import com.thomson.bookingservice.exception.BookingNotFoundException;
import com.thomson.bookingservice.feign.FlightServiceClient;
import com.thomson.bookingservice.publisher.BookingEventPublisher;
import com.thomson.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightServiceClient flightServiceClient;
    private final BookingEventPublisher eventPublisher;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        double actualPrice = flightServiceClient.getFlight(request.getFlightId()).getPrice();
        if (Math.abs(request.getTotalPrice() - actualPrice) > 0.01) {
            throw new IllegalArgumentException(
                "Invalid price. Expected " + actualPrice + " but got " + request.getTotalPrice()
            );
        }

        flightServiceClient.bookSeat(request.getFlightId(), request.getSeatNumber());

        Booking booking = Booking.builder()
                .userId(request.getUserId())
                .flightId(request.getFlightId())
                .seatNumber(request.getSeatNumber())
                .totalPrice(request.getTotalPrice())
                .status(BookingStatus.PENDING)
                .bookingTime(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking " + bookingId + " cannot be confirmed — status is " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publish(BookingEvent.builder()
                .bookingId(saved.getId())
                .userId(saved.getUserId())
                .flightId(saved.getFlightId())
                .seatNumber(saved.getSeatNumber())
                .totalPrice(saved.getTotalPrice())
                .eventType("BOOKING_CONFIRMED")
                .build());

        return mapToResponse(saved);
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
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publish(BookingEvent.builder()
                .bookingId(saved.getId())
                .userId(saved.getUserId())
                .flightId(saved.getFlightId())
                .seatNumber(saved.getSeatNumber())
                .totalPrice(saved.getTotalPrice())
                .eventType("BOOKING_CANCELLED")
                .build());

        return mapToResponse(saved);
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

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireBookings() {
        List<Booking> expired = bookingRepository
                .findByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now());

        for (Booking booking : expired) {
            log.info("Expiring booking {} — payment window elapsed", booking.getId());
            flightServiceClient.releaseSeat(booking.getFlightId(), booking.getSeatNumber());
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            eventPublisher.publish(BookingEvent.builder()
                    .bookingId(booking.getId())
                    .userId(booking.getUserId())
                    .flightId(booking.getFlightId())
                    .seatNumber(booking.getSeatNumber())
                    .totalPrice(booking.getTotalPrice())
                    .eventType("BOOKING_CANCELLED")
                    .build());
        }
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
