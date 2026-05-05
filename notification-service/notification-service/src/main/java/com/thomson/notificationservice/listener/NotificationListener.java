package com.thomson.notificationservice.listener;

import com.thomson.notificationservice.dto.BookingEvent;
import com.thomson.notificationservice.dto.PaymentEvent;
import com.thomson.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.booking-queue}")
    public void handleBookingEvent(BookingEvent event) {
        log.info("Received booking event: {} for booking {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "BOOKING_CONFIRMED" -> emailService.send(
                    resolveEmail(event.getUserId()),
                    "Booking Confirmed — AirSync",
                    "Your booking #" + event.getBookingId() + " for seat " + event.getSeatNumber()
                    + " has been confirmed. Total paid: ₦" + event.getTotalPrice()
            );
            case "BOOKING_CANCELLED" -> emailService.send(
                    resolveEmail(event.getUserId()),
                    "Booking Cancelled — AirSync",
                    "Your booking #" + event.getBookingId() + " for seat " + event.getSeatNumber()
                    + " has been cancelled. Your seat has been released."
            );
            default -> log.warn("Unknown booking event type: {}", event.getEventType());
        }
    }

    @RabbitListener(queues = "${rabbitmq.payment-queue}")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for booking {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "PAYMENT_SUCCESS" -> emailService.send(
                    event.getEmail(),
                    "Payment Receipt — AirSync",
                    "Payment of ₦" + event.getAmount() + " for booking #" + event.getBookingId()
                    + " was successful. Reference: " + event.getPaymentId()
            );
            case "PAYMENT_FAILED" -> emailService.send(
                    event.getEmail(),
                    "Payment Failed — AirSync",
                    "Payment of ₦" + event.getAmount() + " for booking #" + event.getBookingId()
                    + " failed. Please try again or contact support."
            );
            default -> log.warn("Unknown payment event type: {}", event.getEventType());
        }
    }

    // Placeholder — replace with a user-service Feign call to get the user's email
    private String resolveEmail(Long userId) {
        return "user" + userId + "@example.com";
    }
}
